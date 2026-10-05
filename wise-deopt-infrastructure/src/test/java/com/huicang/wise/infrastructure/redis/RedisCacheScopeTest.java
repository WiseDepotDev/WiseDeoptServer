package com.huicang.wise.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.aspect.RedisCacheAspect;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 缓存**清理范围**的回归测试（不需要真 Redis：用 mock 看它到底发了哪些命令）。
 *
 * <p>这一组用例守的是 2026-10-05 实测到的一次事故：`@CacheEvict(allEntries = true)` 当时调用 `flushAll()`（等价 `KEYS *` +
 * `DEL`），于是"新建/修改一个仓库"把同一个 Redis 库里的 **登录失败计数、限流计数、人机验证挑战/票据、请求 nonce** 全部删掉 —— 一次正常业务写， 静默重置了安全边界。
 *
 * <p>所以这里断言的不是"清干净了"，而是**"只清该清的那一类"**： 必须出现 `keys("warehouse:*")`，而且**绝不能**出现 `keys("*")`。
 */
class RedisCacheScopeTest {

    /** 同一个 Redis 库里住着的**非缓存**状态（清理时必须一个都不碰）。 */
    private static final String[] SECURITY_STATE_KEYS = {
        "auth:login:fail:*",
        "auth:login:fail:ip:*",
        "api:rate-limit:*",
        "human:*",
        "api:nonce:*",
        "auth:revoked:*",
        "auth:token:access:*",
    };

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);

    private RedisCacheManager redisCacheManager;

    private RedisCacheAspect aspect;

    private ProceedingJoinPoint joinPoint;

    private MethodSignature signature;

    @BeforeEach
    void setUp() {
        redisCacheManager = new RedisCacheManager(redisTemplate, new ObjectMapper());
        // `RedisCacheUtils` 是静态门面 + Spring 注入（构造器把实例塞进静态字段）。
        // 单元测试里没有容器，所以要**显式**接一次 —— 否则它的静态方法全是 NPE。
        new RedisCacheUtils(redisCacheManager);
        aspect = new RedisCacheAspect();
        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    // ---------------------------------------------------------------- 管理器层

    @Test
    @DisplayName("按前缀清理只发 keys(prefix:*) —— 绝不发 keys(*)")
    void deleteByPatternOnlyTouchesThePrefix() {
        Set<String> matched = new LinkedHashSet<>(Set.of("warehouse:1", "warehouse:2"));
        when(redisTemplate.keys("warehouse:*")).thenReturn(matched);

        redisCacheManager.deleteByPattern("warehouse:*");

        verify(redisTemplate).keys("warehouse:*");
        verify(redisTemplate).delete(matched);
        // 这条断言就是那次事故的反面：整库通配一次都不许出现
        verify(redisTemplate, never()).keys("*");
    }

    @Test
    @DisplayName("空模式 / \"*\" 直接拒（那等于清库，宁可不清也不清多）")
    void deleteByPatternRejectsWholeDatabasePatterns() {
        assertThrows(IllegalArgumentException.class, () -> redisCacheManager.deleteByPattern("*"));
        assertThrows(
                IllegalArgumentException.class, () -> redisCacheManager.deleteByPattern("  *  "));
        assertThrows(IllegalArgumentException.class, () -> redisCacheManager.deleteByPattern(""));
        assertThrows(IllegalArgumentException.class, () -> redisCacheManager.deleteByPattern(null));

        verify(redisTemplate, never()).keys(anyString());
    }

    // ---------------------------------------------------------------- 切面层

    @Test
    @DisplayName("allEntries=true ⇒ 只清该前缀，安全状态一个都不碰")
    void allEntriesEvictsOnlyItsOwnPrefix() throws Throwable {
        givenFixtureMethod("evictWarehouseAll");
        Set<String> matched = new LinkedHashSet<>(Set.of("warehouse:1"));
        when(redisTemplate.keys("warehouse:*")).thenReturn(matched);
        when(joinPoint.proceed()).thenReturn(null);

        aspect.handleCacheEvict(joinPoint, evictOn("evictWarehouseAll"));

        verify(redisTemplate).keys("warehouse:*");
        // 整库通配一次都不许出现；其余"安全状态前缀"由 assertNoSecurityStateTouched 兜住
        verify(redisTemplate, never()).keys("*");
        assertNoSecurityStateTouched();
    }

    @Test
    @DisplayName("allEntries=true 却没写 prefix ⇒ 明确报错，而不是「不知道清什么就全清」")
    void allEntriesWithoutPrefixFailsLoudly() throws Throwable {
        givenFixtureMethod("evictWithoutPrefix");
        when(joinPoint.proceed()).thenReturn(null);

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () -> aspect.handleCacheEvict(joinPoint, evictOn("evictWithoutPrefix")));

        assertTrue(error.getMessage().contains("prefix"), "错误信息要直接说清缺什么，实际是：" + error.getMessage());
        verify(redisTemplate, never()).keys(anyString());
        assertNoSecurityStateTouched();
    }

    @Test
    @DisplayName("精确键的 evict 仍然只删那一个键（改动没有波及普通路径）")
    void singleKeyEvictStillDeletesExactlyOneKey() throws Throwable {
        givenFixtureMethod("evictOneRole", Long.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {7L});
        when(joinPoint.proceed()).thenReturn(null);

        aspect.handleCacheEvict(joinPoint, evictOn("evictOneRole", Long.class));

        // SpEL `#id` 真的按参数解析成了 7
        verify(redisTemplate).delete("role:7");
        verify(redisTemplate, never()).keys(anyString());
    }

    // ---------------------------------------------------------------- 夹具

    private void givenFixtureMethod(String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = RedisCacheScopeTestFixture.class.getDeclaredMethod(name, parameterTypes);
        when(signature.getMethod()).thenReturn(method);
    }

    private void assertNoSecurityStateTouched() {
        for (String key : SECURITY_STATE_KEYS) {
            verify(redisTemplate, never()).keys(key);
        }
    }

    /** 取 [RedisCacheScopeTestFixture] 上的真实注解：不自己 new 一个，那样就测不到注解本身写错了。 */
    private CacheEvict evictOn(String methodName, Class<?>... parameterTypes) {
        try {
            return RedisCacheScopeTestFixture.class
                    .getDeclaredMethod(methodName, parameterTypes)
                    .getAnnotation(CacheEvict.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("测试夹具缺方法 " + methodName, e);
        }
    }

    /** 真实注解的载体。 */
    @SuppressWarnings("unused")
    static class RedisCacheScopeTestFixture {

        @CacheEvict(prefix = "warehouse", allEntries = true)
        void evictWarehouseAll() {}

        @CacheEvict(allEntries = true)
        void evictWithoutPrefix() {}

        @CacheEvict(prefix = "role", key = "#id", allEntries = false)
        void evictOneRole(Long id) {}
    }
}

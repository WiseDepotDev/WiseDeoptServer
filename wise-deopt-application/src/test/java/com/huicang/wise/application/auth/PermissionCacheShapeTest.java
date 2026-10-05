package com.huicang.wise.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huicang.wise.application.role.AssignPermissionsRequest;
import com.huicang.wise.application.role.RoleApplicationService;
import com.huicang.wise.application.user.AssignRolesRequest;
import com.huicang.wise.application.user.UserRoleApplicationService;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvicts;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 权限缓存的**形状**门禁（纯反射，不需要容器）。
 *
 * <p>守的是 2026-10-05 审计发现的一个"看着有缓存、实际没有"： `checkPermission` 原来是 `void`，而 `RedisCacheAspect` 只缓存**非
 * null 的返回值** （`if (result != null && …)`）—— 于是读缓存永远未命中、写缓存永远写不进去， 每个带 `@RequiresPermission` 的请求都要查
 * 4 张表（用户 → 角色 → 角色权限 → 权限）。 注解在场，所以任何人 review 时都会以为它生效了。
 *
 * <p>为什么用反射而不是集成测试：这条约束是"注解 + 方法签名"的**搭配**问题， 只要有人把返回值改回 `void`（或去掉注解），这里立刻变红；而集成测试依赖真 Redis， 在 CI
 * 上未必跑（`@Tag("e2e")` 那批就是）。
 */
class PermissionCacheShapeTest {

    @Test
    @DisplayName("checkPermission 的返回值不能是 void —— 否则 @Cacheable 永远存不下东西")
    void checkPermissionMustHaveCacheableReturnType() throws Exception {
        Method method =
                AuthApplicationService.class.getDeclaredMethod(
                        "checkPermission", String.class, String.class);

        assertNotEquals(
                void.class,
                method.getReturnType(),
                "RedisCacheAspect 只缓存非 null 返回值：void 方法加 @Cacheable 等于没有缓存。"
                        + "权限检查是每个请求都要走的路，这里必须是可缓存的返回值。");

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertTrue(cacheable != null, "权限检查必须带 @Cacheable（否则每请求查 4 张表）");
        assertEquals("auth:permission", cacheable.prefix());
    }

    @Test
    @DisplayName("改权限的四个入口都要让权限缓存失效（否则「刚收回的权限」还能用 15 分钟）")
    void permissionMutatingEntrypointsEvictPermissionCache() throws Exception {
        assertAllEntriesEvict(
                RoleApplicationService.class,
                "assignPermissions",
                Long.class,
                AssignPermissionsRequest.class);
        assertAllEntriesEvict(
                UserRoleApplicationService.class,
                "assignRoles",
                Long.class,
                AssignRolesRequest.class);
        assertAllEntriesEvict(UserRoleApplicationService.class, "removeUserRoles", Long.class);
        assertAllEntriesEvict(
                UserRoleApplicationService.class, "removeUserRole", Long.class, Long.class);
    }

    @Test
    @DisplayName("删角色要同时作废两类缓存（两个 @CacheEvict 会被编译器包进容纳注解）")
    void deleteRoleEvictsBothItsOwnEntryAndPermissions() throws Exception {
        Method method = RoleApplicationService.class.getDeclaredMethod("deleteRole", Long.class);
        CacheEvicts container = method.getAnnotation(CacheEvicts.class);

        assertTrue(
                container != null,
                "deleteRole 应有两个 @CacheEvict（role:<id> 与 auth:permission:*）—— "
                        + "只写一个的话另一类缓存会 stale 到 TTL 到期");

        List<String> prefixes =
                Arrays.stream(container.value())
                        .map(CacheEvict::prefix)
                        .collect(Collectors.toList());
        assertTrue(prefixes.contains("role"), "要失效这个角色自己的缓存，实得 " + prefixes);
        assertTrue(prefixes.contains("auth:permission"), "要失效权限缓存，实得 " + prefixes);
    }

    private void assertAllEntriesEvict(Class<?> type, String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = type.getDeclaredMethod(methodName, parameterTypes);
        CacheEvict evict = method.getAnnotation(CacheEvict.class);
        assertTrue(evict != null, type.getSimpleName() + "." + methodName + " 必须清权限缓存：它改的是「谁能做什么」");
        assertEquals(
                "auth:permission",
                evict.prefix(),
                type.getSimpleName() + "." + methodName + " 应清 auth:permission 前缀");
        assertTrue(
                evict.allEntries(),
                type.getSimpleName() + "." + methodName + " 要整类清（缓存键是 用户名:权限码，改一个角色会波及很多用户）");
    }
}

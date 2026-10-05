package com.huicang.wise.application.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huicang.wise.infrastructure.redis.RedisKeys;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvicts;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 消息的**缓存与失效必须配对**（纯反射门禁）。
 *
 * <p>守的是把消息改成数据库实现时一起补掉的一个陈旧读：`getMessageById` 有 30 分钟缓存， 而"标记已读"原先不会清它 —— 于是用户点开消息、再查一次，拿到的还是那份
 * `isRead=false` 的旧 DTO（界面上表现为"点了已读，红点还在"）。
 *
 * <p>未读数同理，而且它的代价更大：它是桥每 10 秒轮询一次的**最热读**， 缓存一旦不被写侧失效，用户会在最长 60 秒里看到错的未读数。
 *
 * <p>为什么写成反射门禁而不是集成测试：这里要钉的是"注解有没有写对"这个**搭配**问题， 而集成测试依赖真 Redis（`@Tag("e2e")` 那批在 CI 上未必跑）。
 */
class MessageCacheInvalidationTest {

    @Test
    @DisplayName("改消息状态的四个写入口都必须失效缓存（否则读到的还是旧的已读状态）")
    void mutatingEntrypointsEvictMessageCaches() throws Exception {
        // 标记已读：既要失效这条消息的 DTO 缓存，也要失效未读数（它一定是接收者的那个键）
        assertEvicts(
                prefixesOf("markAsRead", String.class),
                RedisKeys.MESSAGE,
                RedisKeys.MESSAGE_UNREAD);
        // 删除单条：同上（参数只有 messageId，所以未读数只能整类清）
        assertEvicts(
                prefixesOf("deleteMessage", String.class),
                RedisKeys.MESSAGE,
                RedisKeys.MESSAGE_UNREAD);
        // 全部已读 / 删全部：只需要未读数（DTO 缓存的键是 messageId，这里没有）
        assertEvicts(prefixesOf("markAllAsRead", Long.class), RedisKeys.MESSAGE_UNREAD);
        assertEvicts(prefixesOf("deleteAllMessages", Long.class), RedisKeys.MESSAGE_UNREAD);
    }

    @Test
    @DisplayName("新增消息要失效该接收者的未读数（否则新消息不涨红点）")
    void createMessageEvictsUnreadCount() throws Exception {
        Method method =
                MessageApplicationService.class.getDeclaredMethod(
                        "createMessage", MessageCreateRequest.class);
        CacheEvict evict = method.getAnnotation(CacheEvict.class);

        assertTrue(evict != null, "createMessage 必须失效未读数缓存");
        assertEquals(RedisKeys.MESSAGE_UNREAD, evict.prefix());
        assertTrue(
                evict.key().contains("receiverId"), "新增消息知道接收者，应当精确失效那一个键，实得 key=" + evict.key());
    }

    @Test
    @DisplayName("未读数是要缓存的（桥每 10s 轮询一次），而且 TTL 要短")
    void unreadCountIsCachedWithShortTtl() throws Exception {
        Method method =
                MessageApplicationService.class.getDeclaredMethod("getUnreadCount", Long.class);

        com.huicang.wise.infrastructure.redis.annotation.Cacheable cacheable =
                method.getAnnotation(
                        com.huicang.wise.infrastructure.redis.annotation.Cacheable.class);
        assertTrue(cacheable != null, "getUnreadCount 是最热的一次读，必须有缓存");
        assertEquals(RedisKeys.MESSAGE_UNREAD, cacheable.prefix());
        assertTrue(
                cacheable.timeout() <= 60,
                "未读数的 TTL 不该超过 60 秒（正确性靠写侧失效，TTL 只是兜底）：实得 " + cacheable.timeout());
    }

    /** 取一个方法上所有 `@CacheEvict` 的 prefix（两个以上时会被编译器包进容纳注解）。 */
    private List<String> prefixesOf(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method =
                MessageApplicationService.class.getDeclaredMethod(methodName, parameterTypes);
        CacheEvict single = method.getAnnotation(CacheEvict.class);
        if (single != null) {
            return List.of(single.prefix());
        }
        CacheEvicts container = method.getAnnotation(CacheEvicts.class);
        assertTrue(container != null, methodName + " 应当带至少一个 @CacheEvict");
        return Arrays.stream(container.value())
                .map(CacheEvict::prefix)
                .collect(Collectors.toList());
    }

    private void assertEvicts(List<String> actual, String... expected) {
        for (String prefix : expected) {
            assertTrue(
                    actual.contains(prefix),
                    "缺少对 " + prefix + " 的失效（实得 " + actual + "）—— " + "缓存不被写侧失效，用户就会读到旧状态");
        }
    }
}

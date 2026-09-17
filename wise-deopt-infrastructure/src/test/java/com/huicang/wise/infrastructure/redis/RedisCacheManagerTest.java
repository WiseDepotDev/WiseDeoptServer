package com.huicang.wise.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis缓存管理器测试类
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-03-21
 */
@Tag("e2e")
@SpringBootTest
class RedisCacheManagerTest {

    @Autowired
    private RedisCacheManager redisCacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_KEY_PREFIX = "test:";

    @BeforeEach
    void setUp() {
        redisCacheManager.deleteByPattern(TEST_KEY_PREFIX + "*");
    }

    @Test
    void testSetAndGet() {
        String key = TEST_KEY_PREFIX + "setAndGet";
        String value = "testValue";

        redisCacheManager.set(key, value);
        Object result = redisCacheManager.get(key);

        assertNotNull(result);
        assertEquals(value, result);
    }

    @Test
    void testSetWithExpire() {
        String key = TEST_KEY_PREFIX + "setWithExpire";
        String value = "testValue";

        redisCacheManager.set(key, value, 2, TimeUnit.SECONDS);
        Object result = redisCacheManager.get(key);

        assertNotNull(result);
        assertEquals(value, result);

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        Object expiredResult = redisCacheManager.get(key);
        assertNull(expiredResult);
    }

    @Test
    void testSetAndGetWithType() {
        String key = TEST_KEY_PREFIX + "setAndGetWithType";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        redisCacheManager.set(key, user);
        TestUser result = redisCacheManager.get(key, TestUser.class);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void testDelete() {
        String key = TEST_KEY_PREFIX + "delete";
        String value = "testValue";

        redisCacheManager.set(key, value);
        assertTrue(redisCacheManager.exists(key));

        redisCacheManager.delete(key);
        assertFalse(redisCacheManager.exists(key));
    }

    @Test
    void testBatchDelete() {
        String key1 = TEST_KEY_PREFIX + "batchDelete1";
        String key2 = TEST_KEY_PREFIX + "batchDelete2";
        String key3 = TEST_KEY_PREFIX + "batchDelete3";

        redisCacheManager.set(key1, "value1");
        redisCacheManager.set(key2, "value2");
        redisCacheManager.set(key3, "value3");

        assertTrue(redisCacheManager.exists(key1));
        assertTrue(redisCacheManager.exists(key2));
        assertTrue(redisCacheManager.exists(key3));

        redisCacheManager.delete(Arrays.asList(key1, key2, key3));

        assertFalse(redisCacheManager.exists(key1));
        assertFalse(redisCacheManager.exists(key2));
        assertFalse(redisCacheManager.exists(key3));
    }

    @Test
    void testExists() {
        String key = TEST_KEY_PREFIX + "exists";

        assertFalse(redisCacheManager.exists(key));

        redisCacheManager.set(key, "value");
        assertTrue(redisCacheManager.exists(key));
    }

    @Test
    void testExpire() {
        String key = TEST_KEY_PREFIX + "expire";
        String value = "testValue";

        redisCacheManager.set(key, value);
        redisCacheManager.expire(key, 2, TimeUnit.SECONDS);

        long expire1 = redisCacheManager.getExpire(key, TimeUnit.SECONDS);
        assertTrue(expire1 > 0);

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        Object expiredResult = redisCacheManager.get(key);
        assertNull(expiredResult);
    }

    @Test
    void testHSetAndGet() {
        String key = TEST_KEY_PREFIX + "hSetAndGet";
        String field = "userField";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        redisCacheManager.hSet(key, field, user);
        Object result = redisCacheManager.hGet(key, field);

        assertNotNull(result);
        TestUser userResult = (TestUser) result;
        assertEquals(user.getUserId(), userResult.getUserId());
        assertEquals(user.getUsername(), userResult.getUsername());
    }

    @Test
    void testHSetAndGetWithType() {
        String key = TEST_KEY_PREFIX + "hSetAndGetWithType";
        String field = "userField";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        redisCacheManager.hSet(key, field, user);
        TestUser result = redisCacheManager.hGet(key, field, TestUser.class);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
    }

    @Test
    void testHDelete() {
        String key = TEST_KEY_PREFIX + "hDelete";
        String field = "userField";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        redisCacheManager.hSet(key, field, user);
        assertTrue(redisCacheManager.hExists(key, field));

        redisCacheManager.hDelete(key, field);
        assertFalse(redisCacheManager.hExists(key, field));
    }

    @Test
    void testHExists() {
        String key = TEST_KEY_PREFIX + "hExists";
        String field = "userField";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        assertFalse(redisCacheManager.hExists(key, field));

        redisCacheManager.hSet(key, field, user);
        assertTrue(redisCacheManager.hExists(key, field));
    }

    @Test
    void testHGetAll() {
        String key = TEST_KEY_PREFIX + "hGetAll";
        TestUser user1 = new TestUser(1L, "张三", "zhangsan@example.com");
        TestUser user2 = new TestUser(2L, "李四", "lisi@example.com");

        redisCacheManager.hSet(key, "user1", user1);
        redisCacheManager.hSet(key, "user2", user2);

        Map<Object, Object> result = redisCacheManager.hGetAll(key);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testLPushAndLRange() {
        String key = TEST_KEY_PREFIX + "lPushAndLRange";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        redisCacheManager.lPush(key, value1);
        redisCacheManager.lPush(key, value2);
        redisCacheManager.lPush(key, value3);

        List<Object> result = redisCacheManager.lRange(key, 0, -1);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(value3, result.get(0));
        assertEquals(value2, result.get(1));
        assertEquals(value1, result.get(2));
    }

    @Test
    void testLSize() {
        String key = TEST_KEY_PREFIX + "lSize";

        assertEquals(0, redisCacheManager.lSize(key));

        redisCacheManager.lPush(key, "value1");
        redisCacheManager.lPush(key, "value2");

        assertEquals(2, redisCacheManager.lSize(key));
    }

    @Test
    void testLRemove() {
        String key = TEST_KEY_PREFIX + "lRemove";
        String value1 = "value1";
        String value2 = "value2";

        redisCacheManager.lPush(key, value1);
        redisCacheManager.lPush(key, value2);
        redisCacheManager.lPush(key, value1);

        assertEquals(3, redisCacheManager.lSize(key));

        redisCacheManager.lRemove(key, 2, value1);

        assertEquals(1, redisCacheManager.lSize(key));
    }

    @Test
    void testSAddAndSMembers() {
        String key = TEST_KEY_PREFIX + "sAddAndSMembers";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        redisCacheManager.sAdd(key, value1, value2, value3);

        Set<Object> result = redisCacheManager.sMembers(key);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(value1));
        assertTrue(result.contains(value2));
        assertTrue(result.contains(value3));
    }

    @Test
    void testSIsMember() {
        String key = TEST_KEY_PREFIX + "sIsMember";
        String value1 = "value1";
        String value2 = "value2";

        redisCacheManager.sAdd(key, value1);

        assertTrue(redisCacheManager.sIsMember(key, value1));
        assertFalse(redisCacheManager.sIsMember(key, value2));
    }

    @Test
    void testSRemove() {
        String key = TEST_KEY_PREFIX + "sRemove";
        String value1 = "value1";
        String value2 = "value2";

        redisCacheManager.sAdd(key, value1, value2);
        assertEquals(2, redisCacheManager.sSize(key));

        redisCacheManager.sRemove(key, value1);
        assertEquals(1, redisCacheManager.sSize(key));
        assertFalse(redisCacheManager.sIsMember(key, value1));
        assertTrue(redisCacheManager.sIsMember(key, value2));
    }

    @Test
    void testSSize() {
        String key = TEST_KEY_PREFIX + "sSize";

        assertEquals(0, redisCacheManager.sSize(key));

        redisCacheManager.sAdd(key, "value1", "value2", "value3");
        assertEquals(3, redisCacheManager.sSize(key));
    }

    @Test
    void testDeleteByPattern() {
        String key1 = TEST_KEY_PREFIX + "pattern1";
        String key2 = TEST_KEY_PREFIX + "pattern2";
        String key3 = TEST_KEY_PREFIX + "other";

        redisCacheManager.set(key1, "value1");
        redisCacheManager.set(key2, "value2");
        redisCacheManager.set(key3, "value3");

        assertTrue(redisCacheManager.exists(key1));
        assertTrue(redisCacheManager.exists(key2));
        assertTrue(redisCacheManager.exists(key3));

        redisCacheManager.deleteByPattern(TEST_KEY_PREFIX + "pattern*");

        assertFalse(redisCacheManager.exists(key1));
        assertFalse(redisCacheManager.exists(key2));
        assertTrue(redisCacheManager.exists(key3));
    }

    static class TestUser {
        private Long userId;
        private String username;
        private String email;

        public TestUser() {
        }

        public TestUser(Long userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
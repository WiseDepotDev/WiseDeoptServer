package com.huicang.wise.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis缓存工具类测试类
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-03-21
 */
@Tag("e2e")
@SpringBootTest
class RedisCacheUtilsTest {

    private static final String TEST_KEY_PREFIX = "test:utils:";

    @BeforeEach
    void setUp() {
        RedisCacheUtils.deleteByPattern(TEST_KEY_PREFIX + "*");
    }

    @Test
    void testSetAndGet() {
        String key = TEST_KEY_PREFIX + "setAndGet";
        String value = "testValue";

        RedisCacheUtils.set(key, value);
        Object result = RedisCacheUtils.get(key);

        assertNotNull(result);
        assertEquals(value, result);
    }

    @Test
    void testSetWithExpire() {
        String key = TEST_KEY_PREFIX + "setWithExpire";
        String value = "testValue";

        RedisCacheUtils.set(key, value, 2, TimeUnit.SECONDS);
        Object result = RedisCacheUtils.get(key);

        assertNotNull(result);
        assertEquals(value, result);

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        Object expiredResult = RedisCacheUtils.get(key);
        assertNull(expiredResult);
    }

    @Test
    void testSetWithExpireInSeconds() {
        String key = TEST_KEY_PREFIX + "setWithExpireInSeconds";
        String value = "testValue";

        RedisCacheUtils.set(key, value, 2);
        Object result = RedisCacheUtils.get(key);

        assertNotNull(result);
        assertEquals(value, result);
    }

    @Test
    void testSetAndGetWithType() {
        String key = TEST_KEY_PREFIX + "setAndGetWithType";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        RedisCacheUtils.set(key, user);
        TestUser result = RedisCacheUtils.get(key, TestUser.class);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void testDelete() {
        String key = TEST_KEY_PREFIX + "delete";
        String value = "testValue";

        RedisCacheUtils.set(key, value);
        assertTrue(RedisCacheUtils.exists(key));

        RedisCacheUtils.delete(key);
        assertFalse(RedisCacheUtils.exists(key));
    }

    @Test
    void testBatchDeleteWithCollection() {
        String key1 = TEST_KEY_PREFIX + "batchDelete1";
        String key2 = TEST_KEY_PREFIX + "batchDelete2";
        String key3 = TEST_KEY_PREFIX + "batchDelete3";

        RedisCacheUtils.set(key1, "value1");
        RedisCacheUtils.set(key2, "value2");
        RedisCacheUtils.set(key3, "value3");

        assertTrue(RedisCacheUtils.exists(key1));
        assertTrue(RedisCacheUtils.exists(key2));
        assertTrue(RedisCacheUtils.exists(key3));

        RedisCacheUtils.delete(Arrays.asList(key1, key2, key3));

        assertFalse(RedisCacheUtils.exists(key1));
        assertFalse(RedisCacheUtils.exists(key2));
        assertFalse(RedisCacheUtils.exists(key3));
    }

    @Test
    void testBatchDeleteWithArray() {
        String key1 = TEST_KEY_PREFIX + "batchDeleteArray1";
        String key2 = TEST_KEY_PREFIX + "batchDeleteArray2";

        RedisCacheUtils.set(key1, "value1");
        RedisCacheUtils.set(key2, "value2");

        assertTrue(RedisCacheUtils.exists(key1));
        assertTrue(RedisCacheUtils.exists(key2));

        RedisCacheUtils.delete(key1, key2);

        assertFalse(RedisCacheUtils.exists(key1));
        assertFalse(RedisCacheUtils.exists(key2));
    }

    @Test
    void testExists() {
        String key = TEST_KEY_PREFIX + "exists";

        assertFalse(RedisCacheUtils.exists(key));

        RedisCacheUtils.set(key, "value");
        assertTrue(RedisCacheUtils.exists(key));
    }

    @Test
    void testExpire() {
        String key = TEST_KEY_PREFIX + "expire";
        String value = "testValue";

        RedisCacheUtils.set(key, value);
        RedisCacheUtils.expire(key, 2, TimeUnit.SECONDS);

        long expire1 = RedisCacheUtils.getExpire(key, TimeUnit.SECONDS);
        assertTrue(expire1 > 0);

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        Object expiredResult = RedisCacheUtils.get(key);
        assertNull(expiredResult);
    }

    @Test
    void testExpireInSeconds() {
        String key = TEST_KEY_PREFIX + "expireInSeconds";
        String value = "testValue";

        RedisCacheUtils.set(key, value);
        RedisCacheUtils.expire(key, 2);

        long expire = RedisCacheUtils.getExpire(key);
        assertTrue(expire > 0);
    }

    @Test
    void testHSetAndGet() {
        String key = TEST_KEY_PREFIX + "hSetAndGet";
        String field = "userField";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        RedisCacheUtils.hSet(key, field, user);
        Object result = RedisCacheUtils.hGet(key, field);

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

        RedisCacheUtils.hSet(key, field, user);
        TestUser result = RedisCacheUtils.hGet(key, field, TestUser.class);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getUsername(), result.getUsername());
    }

    @Test
    void testHDelete() {
        String key = TEST_KEY_PREFIX + "hDelete";
        String field = "userField";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        RedisCacheUtils.hSet(key, field, user);
        assertTrue(RedisCacheUtils.hExists(key, field));

        RedisCacheUtils.hDelete(key, field);
        assertFalse(RedisCacheUtils.hExists(key, field));
    }

    @Test
    void testHExists() {
        String key = TEST_KEY_PREFIX + "hExists";
        String field = "userField";
        TestUser user = new TestUser(1L, "张三", "zhangsan@example.com");

        assertFalse(RedisCacheUtils.hExists(key, field));

        RedisCacheUtils.hSet(key, field, user);
        assertTrue(RedisCacheUtils.hExists(key, field));
    }

    @Test
    void testHGetAll() {
        String key = TEST_KEY_PREFIX + "hGetAll";
        TestUser user1 = new TestUser(1L, "张三", "zhangsan@example.com");
        TestUser user2 = new TestUser(2L, "李四", "lisi@example.com");

        RedisCacheUtils.hSet(key, "user1", user1);
        RedisCacheUtils.hSet(key, "user2", user2);

        Map<Object, Object> result = RedisCacheUtils.hGetAll(key);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testLPushAndLRange() {
        String key = TEST_KEY_PREFIX + "lPushAndLRange";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        RedisCacheUtils.lPush(key, value1);
        RedisCacheUtils.lPush(key, value2);
        RedisCacheUtils.lPush(key, value3);

        List<Object> result = RedisCacheUtils.lRange(key, 0, -1);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(value3, result.get(0));
        assertEquals(value2, result.get(1));
        assertEquals(value1, result.get(2));
    }

    @Test
    void testLSize() {
        String key = TEST_KEY_PREFIX + "lSize";

        assertEquals(0, RedisCacheUtils.lSize(key));

        RedisCacheUtils.lPush(key, "value1");
        RedisCacheUtils.lPush(key, "value2");

        assertEquals(2, RedisCacheUtils.lSize(key));
    }

    @Test
    void testLRemove() {
        String key = TEST_KEY_PREFIX + "lRemove";
        String value1 = "value1";
        String value2 = "value2";

        RedisCacheUtils.lPush(key, value1);
        RedisCacheUtils.lPush(key, value2);
        RedisCacheUtils.lPush(key, value1);

        assertEquals(3, RedisCacheUtils.lSize(key));

        RedisCacheUtils.lRemove(key, 2, value1);

        assertEquals(1, RedisCacheUtils.lSize(key));
    }

    @Test
    void testSAddAndSMembers() {
        String key = TEST_KEY_PREFIX + "sAddAndSMembers";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        RedisCacheUtils.sAdd(key, value1, value2, value3);

        Set<Object> result = RedisCacheUtils.sMembers(key);
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

        RedisCacheUtils.sAdd(key, value1);

        assertTrue(RedisCacheUtils.sIsMember(key, value1));
        assertFalse(RedisCacheUtils.sIsMember(key, value2));
    }

    @Test
    void testSRemove() {
        String key = TEST_KEY_PREFIX + "sRemove";
        String value1 = "value1";
        String value2 = "value2";

        RedisCacheUtils.sAdd(key, value1, value2);
        assertEquals(2, RedisCacheUtils.sSize(key));

        RedisCacheUtils.sRemove(key, value1);
        assertEquals(1, RedisCacheUtils.sSize(key));
        assertFalse(RedisCacheUtils.sIsMember(key, value1));
        assertTrue(RedisCacheUtils.sIsMember(key, value2));
    }

    @Test
    void testSSize() {
        String key = TEST_KEY_PREFIX + "sSize";

        assertEquals(0, RedisCacheUtils.sSize(key));

        RedisCacheUtils.sAdd(key, "value1", "value2", "value3");
        assertEquals(3, RedisCacheUtils.sSize(key));
    }

    @Test
    void testDeleteByPattern() {
        String key1 = TEST_KEY_PREFIX + "pattern1";
        String key2 = TEST_KEY_PREFIX + "pattern2";
        String key3 = TEST_KEY_PREFIX + "other";

        RedisCacheUtils.set(key1, "value1");
        RedisCacheUtils.set(key2, "value2");
        RedisCacheUtils.set(key3, "value3");

        assertTrue(RedisCacheUtils.exists(key1));
        assertTrue(RedisCacheUtils.exists(key2));
        assertTrue(RedisCacheUtils.exists(key3));

        RedisCacheUtils.deleteByPattern(TEST_KEY_PREFIX + "pattern*");

        assertFalse(RedisCacheUtils.exists(key1));
        assertFalse(RedisCacheUtils.exists(key2));
        assertTrue(RedisCacheUtils.exists(key3));
    }

    @Test
    void testGenerateKey() {
        String key1 = RedisCacheUtils.generateKey("user", 1L);
        assertEquals("user:1", key1);

        String key2 = RedisCacheUtils.generateKey("user", 1L, "profile");
        assertEquals("user:1:profile", key2);

        String key3 = RedisCacheUtils.generateKey("user", 1L, "profile", "avatar");
        assertEquals("user:1:profile:avatar", key3);
    }

    @Test
    void testGetOrSet() {
        String key = TEST_KEY_PREFIX + "getOrSet";
        String value = "testValue";

        RedisCacheUtils.DataLoader<String> dataLoader = () -> value;

        String result1 = RedisCacheUtils.getOrSet(key, String.class, dataLoader, 60);
        assertNotNull(result1);
        assertEquals(value, result1);

        String result2 = RedisCacheUtils.getOrSet(key, String.class, dataLoader, 60);
        assertNotNull(result2);
        assertEquals(value, result2);
    }

    @Test
    void testGetOrSetWithNull() {
        String key = TEST_KEY_PREFIX + "getOrSetNull";

        RedisCacheUtils.DataLoader<String> dataLoader = () -> null;

        String result = RedisCacheUtils.getOrSet(key, String.class, dataLoader, 60);
        assertNull(result);

        Object cached = RedisCacheUtils.get(key);
        assertNull(cached);
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
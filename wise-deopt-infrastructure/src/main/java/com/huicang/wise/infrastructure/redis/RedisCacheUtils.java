package com.huicang.wise.infrastructure.redis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Redis缓存工具类 提供静态方法快速操作Redis缓存，简化缓存操作
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-03-21
 */
@Component
public class RedisCacheUtils {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheUtils.class);

    private static RedisCacheManager redisCacheManager;

    public RedisCacheUtils(RedisCacheManager redisCacheManager) {
        RedisCacheUtils.redisCacheManager = redisCacheManager;
    }

    /**
     * 设置缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    public static void set(String key, Object value) {
        redisCacheManager.set(key, value);
    }

    /**
     * 设置缓存并指定过期时间
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public static void set(String key, Object value, long timeout, TimeUnit unit) {
        redisCacheManager.set(key, value, timeout, unit);
    }

    /**
     * 设置缓存并指定过期时间（秒）
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间（秒）
     */
    public static void set(String key, Object value, long timeout) {
        redisCacheManager.set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public static Object get(String key) {
        return redisCacheManager.get(key);
    }

    /**
     * 获取缓存并转换为指定类型
     *
     * @param key 缓存键
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 缓存值
     */
    public static <T> T get(String key, Class<T> clazz) {
        return redisCacheManager.get(key, clazz);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    public static void delete(String key) {
        redisCacheManager.delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     */
    public static void delete(Collection<String> keys) {
        redisCacheManager.delete(keys);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键数组
     */
    public static void delete(String... keys) {
        redisCacheManager.delete(List.of(keys));
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public static boolean exists(String key) {
        return redisCacheManager.exists(key);
    }

    /**
     * 设置过期时间
     *
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public static void expire(String key, long timeout, TimeUnit unit) {
        redisCacheManager.expire(key, timeout, unit);
    }

    /**
     * 设置过期时间（秒）
     *
     * @param key 缓存键
     * @param timeout 过期时间（秒）
     */
    public static void expire(String key, long timeout) {
        redisCacheManager.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取过期时间
     *
     * @param key 缓存键
     * @param unit 时间单位
     * @return 过期时间
     */
    public static long getExpire(String key, TimeUnit unit) {
        return redisCacheManager.getExpire(key, unit);
    }

    /**
     * 获取过期时间（秒）
     *
     * @param key 缓存键
     * @return 过期时间（秒）
     */
    public static long getExpire(String key) {
        return redisCacheManager.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 设置哈希缓存
     *
     * @param key 缓存键
     * @param field 哈希字段
     * @param value 哈希值
     */
    public static void hSet(String key, String field, Object value) {
        redisCacheManager.hSet(key, field, value);
    }

    /**
     * 获取哈希缓存
     *
     * @param key 缓存键
     * @param field 哈希字段
     * @return 哈希值
     */
    public static Object hGet(String key, String field) {
        return redisCacheManager.hGet(key, field);
    }

    /**
     * 获取哈希缓存并转换为指定类型
     *
     * @param key 缓存键
     * @param field 哈希字段
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 哈希值
     */
    public static <T> T hGet(String key, String field, Class<T> clazz) {
        return redisCacheManager.hGet(key, field, clazz);
    }

    /**
     * 删除哈希字段
     *
     * @param key 缓存键
     * @param field 哈希字段
     */
    public static void hDelete(String key, String field) {
        redisCacheManager.hDelete(key, field);
    }

    /**
     * 判断哈希字段是否存在
     *
     * @param key 缓存键
     * @param field 哈希字段
     * @return 是否存在
     */
    public static boolean hExists(String key, String field) {
        return redisCacheManager.hExists(key, field);
    }

    /**
     * 获取哈希所有字段和值
     *
     * @param key 缓存键
     * @return 哈希所有字段和值
     */
    public static Map<Object, Object> hGetAll(String key) {
        return redisCacheManager.hGetAll(key);
    }

    /**
     * 设置列表缓存
     *
     * @param key 缓存键
     * @param value 列表值
     */
    public static void lPush(String key, Object value) {
        redisCacheManager.lPush(key, value);
    }

    /**
     * 获取列表缓存
     *
     * @param key 缓存键
     * @param start 开始位置
     * @param end 结束位置
     * @return 列表值
     */
    public static List<Object> lRange(String key, long start, long end) {
        return redisCacheManager.lRange(key, start, end);
    }

    /**
     * 获取列表长度
     *
     * @param key 缓存键
     * @return 列表长度
     */
    public static long lSize(String key) {
        return redisCacheManager.lSize(key);
    }

    /**
     * 删除列表元素
     *
     * @param key 缓存键
     * @param count 删除数量
     * @param value 列表值
     */
    public static void lRemove(String key, long count, Object value) {
        redisCacheManager.lRemove(key, count, value);
    }

    /**
     * 设置集合缓存
     *
     * @param key 缓存键
     * @param values 集合值
     */
    public static void sAdd(String key, Object... values) {
        redisCacheManager.sAdd(key, values);
    }

    /**
     * 获取集合缓存
     *
     * @param key 缓存键
     * @return 集合值
     */
    public static Set<Object> sMembers(String key) {
        return redisCacheManager.sMembers(key);
    }

    /**
     * 判断集合成员是否存在
     *
     * @param key 缓存键
     * @param value 集合值
     * @return 是否存在
     */
    public static boolean sIsMember(String key, Object value) {
        return redisCacheManager.sIsMember(key, value);
    }

    /**
     * 删除集合成员
     *
     * @param key 缓存键
     * @param values 集合值
     */
    public static void sRemove(String key, Object... values) {
        redisCacheManager.sRemove(key, values);
    }

    /**
     * 获取集合大小
     *
     * @param key 缓存键
     * @return 集合大小
     */
    public static long sSize(String key) {
        return redisCacheManager.sSize(key);
    }

    /**
     * 根据模式删除缓存
     *
     * <p>这是"批量清理"的**唯一**入口：`KEYS pattern` + `DEL` 的破坏范围由调用方给出的 模式决定，看得见、收得住。曾经还有一个 `flushAll()`（等价于
     * `KEYS *`）， 它被 `@CacheEvict(allEntries = true)` 调用，把登录锁定、限流、人机验证等 **不属于缓存的安全状态**也一起删了 ——
     * 那个方法已删除，不要加回来。
     *
     * @param pattern 缓存键模式
     */
    public static void deleteByPattern(String pattern) {
        redisCacheManager.deleteByPattern(pattern);
    }

    /**
     * 生成缓存键
     *
     * @param prefix 前缀
     * @param params 参数
     * @return 缓存键
     */
    public static String generateKey(String prefix, Object... params) {
        StringBuilder keyBuilder = new StringBuilder(prefix);
        for (Object param : params) {
            keyBuilder.append(":").append(param);
        }
        return keyBuilder.toString();
    }

    /**
     * 获取或设置缓存 如果缓存不存在，则从数据源获取并设置缓存
     *
     * @param key 缓存键
     * @param clazz 目标类型
     * @param dataLoader 数据加载器
     * @param timeout 过期时间
     * @param <T> 泛型类型
     * @return 缓存值
     */
    public static <T> T getOrSet(
            String key, Class<T> clazz, DataLoader<T> dataLoader, long timeout) {
        T value = get(key, clazz);
        if (value == null) {
            value = dataLoader.load();
            if (value != null) {
                set(key, value, timeout);
            }
        }
        return value;
    }

    /**
     * 数据加载器接口
     *
     * @param <T> 数据类型
     */
    @FunctionalInterface
    public interface DataLoader<T> {
        T load();
    }
}

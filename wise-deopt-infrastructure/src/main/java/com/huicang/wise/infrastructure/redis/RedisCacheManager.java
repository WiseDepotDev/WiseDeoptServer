package com.huicang.wise.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.common.api.ErrorCode;

/**
 * Redis缓存管理器
 * 提供统一的缓存操作接口，支持对象、字符串、列表、集合、哈希等数据结构的缓存操作
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-03-21
 */
@Component
public class RedisCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheManager.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheManager(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            logger.debug("设置缓存成功，key: {}", key);
        } catch (Exception e) {
            logger.error("设置缓存失败，key: {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设置缓存失败", e);
        }
    }

    /**
     * 设置缓存并指定过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            logger.debug("设置缓存成功，key: {}, timeout: {} {}", key, timeout, unit);
        } catch (Exception e) {
            logger.error("设置缓存失败，key: {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "设置缓存失败", e);
        }
    }

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            logger.debug("获取缓存成功，key: {}, value: {}", key, value);
            return value;
        } catch (Exception e) {
            logger.error("获取缓存失败，key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取缓存并转换为指定类型
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            logger.error("获取缓存失败，key: {}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            logger.debug("删除缓存成功，key: {}", key);
        } catch (Exception e) {
            logger.error("删除缓存失败，key: {}", key, e);
        }
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     */
    public void delete(Collection<String> keys) {
        try {
            redisTemplate.delete(keys);
            logger.debug("批量删除缓存成功，keys: {}", keys);
        } catch (Exception e) {
            logger.error("批量删除缓存失败，keys: {}", keys, e);
        }
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return result != null && result;
        } catch (Exception e) {
            logger.error("判断缓存是否存在失败，key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
            logger.debug("设置过期时间成功，key: {}, timeout: {} {}", key, timeout, unit);
        } catch (Exception e) {
            logger.error("设置过期时间失败，key: {}", key, e);
        }
    }

    /**
     * 获取过期时间
     *
     * @param key 缓存键
     * @param unit 时间单位
     * @return 过期时间
     */
    public long getExpire(String key, TimeUnit unit) {
        try {
            Long expire = redisTemplate.getExpire(key, unit);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            logger.error("获取过期时间失败，key: {}", key, e);
            return -1;
        }
    }

    /**
     * 设置哈希缓存
     *
     * @param key   缓存键
     * @param field 哈希字段
     * @param value 哈希值
     */
    public void hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            logger.debug("设置哈希缓存成功，key: {}, field: {}", key, field);
        } catch (Exception e) {
            logger.error("设置哈希缓存失败，key: {}, field: {}", key, field, e);
        }
    }

    /**
     * 获取哈希缓存
     *
     * @param key   缓存键
     * @param field 哈希字段
     * @return 哈希值
     */
    public Object hGet(String key, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            logger.debug("获取哈希缓存成功，key: {}, field: {}", key, field);
            return value;
        } catch (Exception e) {
            logger.error("获取哈希缓存失败，key: {}, field: {}", key, field, e);
            return null;
        }
    }

    /**
     * 获取哈希缓存并转换为指定类型
     *
     * @param key   缓存键
     * @param field 哈希字段
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 哈希值
     */
    public <T> T hGet(String key, String field, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            if (value == null) {
                return null;
            }
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            logger.error("获取哈希缓存失败，key: {}, field: {}", key, field, e);
            return null;
        }
    }

    /**
     * 删除哈希字段
     *
     * @param key   缓存键
     * @param field 哈希字段
     */
    public void hDelete(String key, String field) {
        try {
            redisTemplate.opsForHash().delete(key, field);
            logger.debug("删除哈希字段成功，key: {}, field: {}", key, field);
        } catch (Exception e) {
            logger.error("删除哈希字段失败，key: {}, field: {}", key, field, e);
        }
    }

    /**
     * 判断哈希字段是否存在
     *
     * @param key   缓存键
     * @param field 哈希字段
     * @return 是否存在
     */
    public boolean hExists(String key, String field) {
        try {
            Boolean result = redisTemplate.opsForHash().hasKey(key, field);
            return result != null && result;
        } catch (Exception e) {
            logger.error("判断哈希字段是否存在失败，key: {}, field: {}", key, field, e);
            return false;
        }
    }

    /**
     * 获取哈希所有字段和值
     *
     * @param key 缓存键
     * @return 哈希所有字段和值
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
            logger.debug("获取哈希所有字段和值成功，key: {}", key);
            return map;
        } catch (Exception e) {
            logger.error("获取哈希所有字段和值失败，key: {}", key, e);
            return Map.of();
        }
    }

    /**
     * 设置列表缓存
     *
     * @param key   缓存键
     * @param value 列表值
     */
    public void lPush(String key, Object value) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            logger.debug("设置列表缓存成功，key: {}", key);
        } catch (Exception e) {
            logger.error("设置列表缓存失败，key: {}", key, e);
        }
    }

    /**
     * 获取列表缓存
     *
     * @param key   缓存键
     * @param start 开始位置
     * @param end   结束位置
     * @return 列表值
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            List<Object> list = redisTemplate.opsForList().range(key, start, end);
            logger.debug("获取列表缓存成功，key: {}", key);
            return list;
        } catch (Exception e) {
            logger.error("获取列表缓存失败，key: {}", key, e);
            return List.of();
        }
    }

    /**
     * 获取列表长度
     *
     * @param key 缓存键
     * @return 列表长度
     */
    public long lSize(String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            logger.error("获取列表长度失败，key: {}", key, e);
            return 0;
        }
    }

    /**
     * 删除列表元素
     *
     * @param key   缓存键
     * @param count 删除数量
     * @param value 列表值
     */
    public void lRemove(String key, long count, Object value) {
        try {
            redisTemplate.opsForList().remove(key, count, value);
            logger.debug("删除列表元素成功，key: {}", key);
        } catch (Exception e) {
            logger.error("删除列表元素失败，key: {}", key, e);
        }
    }

    /**
     * 设置集合缓存
     *
     * @param key    缓存键
     * @param values 集合值
     */
    public void sAdd(String key, Object... values) {
        try {
            redisTemplate.opsForSet().add(key, values);
            logger.debug("设置集合缓存成功，key: {}", key);
        } catch (Exception e) {
            logger.error("设置集合缓存失败，key: {}", key, e);
        }
    }

    /**
     * 获取集合缓存
     *
     * @param key 缓存键
     * @return 集合值
     */
    public Set<Object> sMembers(String key) {
        try {
            Set<Object> set = redisTemplate.opsForSet().members(key);
            logger.debug("获取集合缓存成功，key: {}", key);
            return set;
        } catch (Exception e) {
            logger.error("获取集合缓存失败，key: {}", key, e);
            return Set.of();
        }
    }

    /**
     * 判断集合成员是否存在
     *
     * @param key   缓存键
     * @param value 集合值
     * @return 是否存在
     */
    public boolean sIsMember(String key, Object value) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(key, value);
            return result != null && result;
        } catch (Exception e) {
            logger.error("判断集合成员是否存在失败，key: {}", key, e);
            return false;
        }
    }

    /**
     * 删除集合成员
     *
     * @param key    缓存键
     * @param values 集合值
     */
    public void sRemove(String key, Object... values) {
        try {
            redisTemplate.opsForSet().remove(key, values);
            logger.debug("删除集合成员成功，key: {}", key);
        } catch (Exception e) {
            logger.error("删除集合成员失败，key: {}", key, e);
        }
    }

    /**
     * 获取集合大小
     *
     * @param key 缓存键
     * @return 集合大小
     */
    public long sSize(String key) {
        try {
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            logger.error("获取集合大小失败，key: {}", key, e);
            return 0;
        }
    }

    /**
     * 清空所有缓存
     */
    public void flushAll() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("清空所有缓存成功");
            }
        } catch (Exception e) {
            logger.error("清空所有缓存失败", e);
        }
    }

    /**
     * 根据模式删除缓存
     *
     * @param pattern 缓存键模式
     */
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("根据模式删除缓存成功，pattern: {}", pattern);
            }
        } catch (Exception e) {
            logger.error("根据模式删除缓存失败，pattern: {}", pattern, e);
        }
    }
}

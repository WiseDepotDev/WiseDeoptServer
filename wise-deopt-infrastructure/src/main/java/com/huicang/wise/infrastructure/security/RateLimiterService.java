package com.huicang.wise.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimiterService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "api:rate-limit:";

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setScriptText(
                "local key = KEYS[1]\n" +
                "local limit = tonumber(ARGV[1])\n" +
                "local window = tonumber(ARGV[2])\n" +
                "local current = redis.call('incr', key)\n" +
                "if current == 1 then\n" +
                "    redis.call('expire', key, window)\n" +
                "end\n" +
                "if current > limit then\n" +
                "    return 0\n" +
                "else\n" +
                "    return 1\n" +
                "end"
        );
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    public boolean allowRequest(String key, int limit, int windowSeconds) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            Long result = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    Collections.singletonList(redisKey),
                    String.valueOf(limit),
                    String.valueOf(windowSeconds)
            );
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("限流检查失败", e);
            return true;
        }
    }

    public boolean allowByIp(String ip, int limit, int windowSeconds) {
        String key = "ip:" + ip;
        return allowRequest(key, limit, windowSeconds);
    }

    public boolean allowByUser(Long userId, int limit, int windowSeconds) {
        String key = "user:" + userId;
        return allowRequest(key, limit, windowSeconds);
    }

    public boolean allowByApi(String api, int limit, int windowSeconds) {
        String key = "api:" + api;
        return allowRequest(key, limit, windowSeconds);
    }

    public boolean allowByIpAndApi(String ip, String api, int limit, int windowSeconds) {
        String key = "ip:" + ip + ":api:" + api;
        return allowRequest(key, limit, windowSeconds);
    }

    public boolean allowByUserAndApi(Long userId, String api, int limit, int windowSeconds) {
        String key = "user:" + userId + ":api:" + api;
        return allowRequest(key, limit, windowSeconds);
    }

    public void resetLimit(String key) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("重置限流失败", e);
        }
    }
}

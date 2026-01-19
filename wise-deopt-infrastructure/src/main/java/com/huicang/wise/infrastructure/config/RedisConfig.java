package com.huicang.wise.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 类功能描述：Redis配置类
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 配置基础Redis模板
 */
@Configuration
public class RedisConfig {

    /**
     * 方法功能描述：创建StringRedisTemplate
     *
     * @param connectionFactory Redis连接工厂
     * @return 字符串Redis模板
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}


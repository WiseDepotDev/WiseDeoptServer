package com.huicang.wise.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * 类功能描述：Jackson配置，确保空值也能被序列化
 *
 * @author xingchentye
 * @date 2026-02-27
 */
@Configuration
public class JacksonConfiguration {

    /**
     * 方法功能描述：配置ObjectMapper
     *
     * @param builder Jackson构建器
     * @return ObjectMapper实例
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        return objectMapper;
    }
}

package com.huicang.wise.infrastructure.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 类功能描述：MinIO客户端配置
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 创建MinIO客户端Bean
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    /**
     * 方法功能描述：构建MinIO客户端
     *
     * @param properties MinIO配置属性
     * @return MinIO客户端对象
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}


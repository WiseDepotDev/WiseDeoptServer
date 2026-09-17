package com.huicang.wise.infrastructure.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO客户端配置
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-02-27
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(prefix = "spring.minio", name = "endpoint", matchIfMissing = true)
public class MinioConfig {

    /**
     * 构建MinIO客户端
     *
     * @param properties MinIO配置属性
     * @return MinIO客户端对象
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        String endpoint = properties.getEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("MinIO endpoint cannot be null or empty");
        }
        
        // 如果 endpoint 没有协议前缀，添加默认的 http://
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            endpoint = "http://" + endpoint;
        }
        
        // 解析 endpoint URL
        try {
            java.net.URL url = new java.net.URL(endpoint);
            String scheme = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();
            
            // 如果没有指定端口，使用默认端口
            if (port == -1) {
                port = "https".equals(scheme) ? 443 : 9000;
            }
            
            boolean secure = "https".equals(scheme);
            
            return MinioClient.builder()
                    .endpoint(host, port, secure)
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .build();
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Invalid MinIO endpoint URL: " + endpoint, e);
        }
    }
}

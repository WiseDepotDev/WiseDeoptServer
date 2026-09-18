package com.huicang.wise.infrastructure.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.common.log.ErrorLogStorageService;
import com.huicang.wise.common.log.ServerErrorLogDTO;
import com.huicang.wise.infrastructure.config.MinioProperties;
import io.minio.*;
import io.minio.messages.Expiration;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.LifecycleRule;
import io.minio.messages.RuleFilter;
import io.minio.messages.Status;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 基于MinIO的错误日志存储服务实现
 *
 * @author WiseDepot
 * @version 1.0.0
 * @since 2026-03-09
 */
@Service
@ConditionalOnBean(MinioClient.class)
public class MinioErrorLogStorageService implements ErrorLogStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioErrorLogStorageService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public MinioErrorLogStorageService(
            MinioClient minioClient, MinioProperties minioProperties, ObjectMapper objectMapper) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (minioClient == null) {
            log.warn("MinIO客户端未启用，错误日志存储功能不可用");
            return;
        }
        try {
            String bucketName = minioProperties.getErrorLogBucket();
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                log.info("创建错误日志存储桶: {}", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());

                // 设置生命周期策略：7天过期
                log.info("配置错误日志存储桶生命周期策略: 7天自动清理");
                configureLifecycle(bucketName);
            }
        } catch (Exception e) {
            log.error("初始化错误日志存储桶失败", e);
        }
    }

    private void configureLifecycle(String bucketName) {
        try {
            java.time.ZonedDateTime expirationDate =
                    java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                            .plusDays(7)
                            .with(java.time.LocalTime.MIDNIGHT);

            LifecycleRule rule =
                    new LifecycleRule(
                            Status.ENABLED,
                            null,
                            new Expiration(expirationDate, null, null),
                            new RuleFilter("error-"),
                            "expire-7-days",
                            null,
                            null,
                            null);

            LifecycleConfiguration config =
                    new LifecycleConfiguration(Collections.singletonList(rule));

            minioClient.setBucketLifecycle(
                    SetBucketLifecycleArgs.builder().bucket(bucketName).config(config).build());
        } catch (Exception e) {
            log.error("配置存储桶生命周期失败", e);
        }
    }

    @Override
    @Async
    public void storeErrorLog(ServerErrorLogDTO logDTO) {
        if (minioClient == null) {
            log.warn("MinIO客户端不可用，跳过错误日志存储");
            return;
        }

        try {
            String bucketName = minioProperties.getErrorLogBucket();
            String dateStr = logDTO.getTimestamp().format(DATE_FORMATTER);
            String fileName =
                    String.format("error-%s-%s.json", dateStr, UUID.randomUUID().toString());

            String jsonContent = objectMapper.writeValueAsString(logDTO);
            byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes);

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                                    inputStream, contentBytes.length, -1)
                            .contentType("application/json")
                            .build());

            log.debug("错误日志已存储至MinIO: {}/{}", bucketName, fileName);
        } catch (Exception e) {
            log.error("存储错误日志失败", e);
            // 这里不抛出异常，避免影响主流程，但会记录日志
        }
    }

    @Override
    public boolean checkHealth() {
        if (minioClient == null) {
            return false;
        }
        try {
            // 简单列出桶列表来检查连接
            minioClient.listBuckets();
            return true;
        } catch (Exception e) {
            log.error("MinIO健康检查失败", e);
            return false;
        }
    }
}

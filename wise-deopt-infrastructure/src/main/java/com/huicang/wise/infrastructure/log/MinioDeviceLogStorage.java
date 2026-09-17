package com.huicang.wise.infrastructure.log;

import com.huicang.wise.domain.service.DeviceLogStorage;
import com.huicang.wise.infrastructure.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketLifecycleArgs;
import io.minio.messages.Expiration;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.LifecycleRule;
import io.minio.messages.RuleFilter;
import io.minio.messages.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * MinIO设备日志存储实现
 *
 * @author WiseDepot
 * @version 1.0.0
 * @since 2026-03-13
 */
@Slf4j
@Service
@ConditionalOnBean(MinioClient.class)
public class MinioDeviceLogStorage implements DeviceLogStorage {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Autowired
    public MinioDeviceLogStorage(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @PostConstruct
    public void init() {
        if (minioClient == null) return;
        try {
            String bucketName = minioProperties.getDeviceLogBucket();
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                log.info("创建设备日志存储桶: {}", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                configureLifecycle(bucketName);
            }
        } catch (Exception e) {
            log.error("初始化设备日志存储桶失败", e);
        }
    }

    private void configureLifecycle(String bucketName) {
        try {
            java.time.ZonedDateTime expirationDate = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                    .plusDays(30)
                    .with(java.time.LocalTime.MIDNIGHT);
            
            LifecycleRule rule = new LifecycleRule(
                    Status.ENABLED,
                    null,
                    new Expiration(expirationDate, null, null),
                    new RuleFilter(""),
                    "expire-30-days",
                    null,
                    null,
                    null
            );

            LifecycleConfiguration config = new LifecycleConfiguration(Collections.singletonList(rule));
            
            minioClient.setBucketLifecycle(
                    SetBucketLifecycleArgs.builder()
                            .bucket(bucketName)
                            .config(config)
                            .build()
            );
        } catch (Exception e) {
            log.error("配置存储桶生命周期失败", e);
        }
    }

    @Override
    public void storeLog(String deviceCode, String fileName, InputStream content, long size) {
        if (minioClient == null) {
            log.warn("MinIO客户端未启用，无法存储设备日志");
            return;
        }
        try {
            String bucket = minioProperties.getDeviceLogBucket();
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String objectName = String.format("%s/%s/%s", deviceCode, date, fileName);
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(content, size, -1)
                    .contentType("text/plain")
                    .build()
            );
            log.info("已存储设备日志: {}/{}", bucket, objectName);
        } catch (Exception e) {
            log.error("存储设备日志失败", e);
            throw new RuntimeException("存储设备日志失败", e);
        }
    }
}

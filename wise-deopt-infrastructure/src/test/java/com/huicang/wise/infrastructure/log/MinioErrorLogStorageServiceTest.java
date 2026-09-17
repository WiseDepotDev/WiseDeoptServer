package com.huicang.wise.infrastructure.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.huicang.wise.common.log.ServerErrorLogDTO;
import com.huicang.wise.infrastructure.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MinioErrorLogStorageService单元测试
 *
 * @author WiseDepot
 * @version 1.0.0
 * @since 2026-03-09
 */
@ExtendWith(MockitoExtension.class)
public class MinioErrorLogStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties minioProperties;

    private MinioErrorLogStorageService storageService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        storageService = new MinioErrorLogStorageService(minioClient, minioProperties, objectMapper);
    }

    @Test
    public void testStoreErrorLog_Normal() throws Exception {
        // Arrange
        when(minioProperties.getErrorLogBucket()).thenReturn("wise-depot-error-logs");
        
        ServerErrorLogDTO logDTO = ServerErrorLogDTO.builder()
                .stackTrace("java.lang.NullPointerException\n\tat com.example.Test.method(Test.java:10)")
                .requestUrl("/api/test")
                .requestMethod("POST")
                .requestHeaders(Collections.singletonMap("Content-Type", "application/json"))
                .requestBody("{\"key\":\"value\"}")
                .timestamp(LocalDateTime.now())
                .serviceInstanceId("instance-1")
                .build();

        // Act
        storageService.storeErrorLog(logDTO);

        // Assert
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    public void testStoreErrorLog_MinioUnavailable() throws Exception {
        // Arrange
        when(minioProperties.getErrorLogBucket()).thenReturn("wise-depot-error-logs");
        
        ServerErrorLogDTO logDTO = ServerErrorLogDTO.builder()
                .stackTrace("Error")
                .timestamp(LocalDateTime.now())
                .build();

        doThrow(new RuntimeException("MinIO connection failed")).when(minioClient).putObject(any(PutObjectArgs.class));

        // Act
        storageService.storeErrorLog(logDTO);

        // Assert
        // Should not throw exception, but log error
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    public void testStoreErrorLog_LargeStackTrace() throws Exception {
        // Arrange
        when(minioProperties.getErrorLogBucket()).thenReturn("wise-depot-error-logs");
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("at com.example.Test.method(Test.java:").append(i).append(")\n");
        }
        String largeStackTrace = sb.toString();

        ServerErrorLogDTO logDTO = ServerErrorLogDTO.builder()
                .stackTrace(largeStackTrace)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        storageService.storeErrorLog(logDTO);

        // Assert
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }
}

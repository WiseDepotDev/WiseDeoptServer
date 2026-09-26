package com.huicang.wise.api.controller;

import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.log.ErrorLogStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MinIO健康检查控制器
 *
 * @author WiseDepot
 * @version 1.0.0
 * @since 2026-03-09
 */
@Tag(name = "MinIO健康检查", description = "MinIO服务健康检查接口")
@RestController
@RequestMapping("/api/health/minio")
public class MinioHealthController {

    @Autowired(required = false)
    private ErrorLogStorageService errorLogStorageService;

    @Operation(summary = "检查MinIO连接状态")
    @GetMapping
    public ApiResponse<Boolean> checkHealth() {
        if (errorLogStorageService == null) {
            return ApiResponse.success(false);
        }
        boolean isHealthy = errorLogStorageService.checkHealth();
        return ApiResponse.success(isHealthy);
    }
}

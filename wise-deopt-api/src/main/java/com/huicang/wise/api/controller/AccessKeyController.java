package com.huicang.wise.api.controller;

import com.huicang.wise.application.accesskey.*;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "访问密钥管理接口")
@RestController
@RequestMapping("/api/access-keys")
public class AccessKeyController {

    private final AccessKeyApplicationService accessKeyApplicationService;

    public AccessKeyController(AccessKeyApplicationService accessKeyApplicationService) {
        this.accessKeyApplicationService = accessKeyApplicationService;
    }

    @Operation(summary = "获取用户的访问密钥列表", description = "返回指定用户的所有访问密钥")
    @GetMapping("/user/{userId}")
    public ApiResponse<List<AccessKeyDTO>> getUserAccessKeys(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        return ApiResponse.success(accessKeyApplicationService.getUserAccessKeys(userId));
    }

    @Operation(summary = "根据ID获取访问密钥", description = "返回指定ID的访问密钥详情")
    @GetMapping("/{keyId}")
    public ApiResponse<AccessKeyDTO> getAccessKeyById(
            @Parameter(description = "密钥ID", required = true)
            @PathVariable Long keyId) {
        return ApiResponse.success(accessKeyApplicationService.getAccessKeyById(keyId));
    }

    @Operation(summary = "创建访问密钥", description = "为指定用户创建新的访问密钥")
    @PostMapping("/user/{userId}")
    public ApiResponse<AccessKeyDTO> createAccessKey(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "密钥信息", required = true)
            @Valid @RequestBody CreateAccessKeyRequest request) {
        return ApiResponse.success(accessKeyApplicationService.createAccessKey(userId, request));
    }

    @Operation(summary = "启用访问密钥", description = "启用指定ID的访问密钥")
    @PutMapping("/{keyId}/enable")
    public ApiResponse<Void> enableAccessKey(
            @Parameter(description = "密钥ID", required = true)
            @PathVariable Long keyId) {
        accessKeyApplicationService.enableAccessKey(keyId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "禁用访问密钥", description = "禁用指定ID的访问密钥")
    @PutMapping("/{keyId}/disable")
    public ApiResponse<Void> disableAccessKey(
            @Parameter(description = "密钥ID", required = true)
            @PathVariable Long keyId) {
        accessKeyApplicationService.disableAccessKey(keyId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除访问密钥", description = "删除指定ID的访问密钥")
    @DeleteMapping("/{keyId}")
    public ApiResponse<Void> deleteAccessKey(
            @Parameter(description = "密钥ID", required = true)
            @PathVariable Long keyId) {
        accessKeyApplicationService.deleteAccessKey(keyId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取访问密钥的审计日志", description = "返回指定密钥的所有访问日志")
    @GetMapping("/{keyId}/audit-logs")
    public ApiResponse<List<AccessKeyAuditLogDTO>> getAccessKeyAuditLogs(
            @Parameter(description = "密钥ID", required = true)
            @PathVariable Long keyId) {
        return ApiResponse.success(accessKeyApplicationService.getAccessKeyAuditLogs(keyId));
    }
}


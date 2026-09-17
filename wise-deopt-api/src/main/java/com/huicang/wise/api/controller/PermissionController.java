package com.huicang.wise.api.controller;

import com.huicang.wise.application.permission.*;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "权限管理接口")
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionApplicationService permissionApplicationService;

    public PermissionController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    @Operation(summary = "获取所有权限", description = "返回系统中所有权限的列表")
    @GetMapping
    public ApiResponse<List<PermissionDTO>> getAllPermissions() {
        return ApiResponse.success(permissionApplicationService.getAllPermissions());
    }

    @Operation(summary = "获取权限树", description = "返回权限的树形结构")
    @GetMapping("/tree")
    public ApiResponse<List<PermissionDTO>> getPermissionTree() {
        return ApiResponse.success(permissionApplicationService.getPermissionTree());
    }

    @Operation(summary = "根据ID获取权限", description = "根据权限ID返回权限详情")
    @GetMapping("/{id}")
    public ApiResponse<PermissionDTO> getPermissionById(
            @Parameter(description = "权限ID", required = true)
            @PathVariable Long id) {
        return ApiResponse.success(permissionApplicationService.getPermissionById(id));
    }

    @Operation(summary = "根据编码获取权限", description = "根据权限编码返回权限详情")
    @GetMapping("/code/{code}")
    public ApiResponse<PermissionDTO> getPermissionByCode(
            @Parameter(description = "权限编码", required = true)
            @PathVariable String code) {
        return ApiResponse.success(permissionApplicationService.getPermissionByCode(code));
    }

    @Operation(summary = "创建权限", description = "创建新的权限")
    @PostMapping
    public ApiResponse<PermissionDTO> createPermission(
            @Parameter(description = "权限信息", required = true)
            @Valid @RequestBody CreatePermissionRequest request) {
        return ApiResponse.success(permissionApplicationService.createPermission(request));
    }

    @Operation(summary = "更新权限", description = "更新指定ID的权限信息")
    @PutMapping("/{id}")
    public ApiResponse<PermissionDTO> updatePermission(
            @Parameter(description = "权限ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "权限信息", required = true)
            @Valid @RequestBody UpdatePermissionRequest request) {
        return ApiResponse.success(permissionApplicationService.updatePermission(id, request));
    }

    @Operation(summary = "删除权限", description = "删除指定ID的权限")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePermission(
            @Parameter(description = "权限ID", required = true)
            @PathVariable Long id) {
        permissionApplicationService.deletePermission(id);
        return ApiResponse.success(null);
    }
}


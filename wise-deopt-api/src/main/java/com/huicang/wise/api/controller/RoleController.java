package com.huicang.wise.api.controller;

import com.huicang.wise.application.permission.PermissionDTO;
import com.huicang.wise.application.role.*;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "角色管理接口")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleApplicationService roleApplicationService;

    public RoleController(RoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    @Operation(summary = "获取所有角色", description = "返回系统中所有角色的列表")
    @GetMapping
    public ApiResponse<List<RoleDTO>> getAllRoles() {
        return ApiResponse.success(roleApplicationService.getAllRoles());
    }

    @Operation(summary = "根据ID获取角色", description = "根据角色ID返回角色详情，包含权限信息")
    @GetMapping("/{id}")
    public ApiResponse<RoleDTO> getRoleById(
            @Parameter(description = "角色ID", required = true)
            @PathVariable Long id) {
        return ApiResponse.success(roleApplicationService.getRoleById(id));
    }

    @Operation(summary = "根据名称获取角色", description = "根据角色名称返回角色详情，包含权限信息")
    @GetMapping("/name/{name}")
    public ApiResponse<RoleDTO> getRoleByName(
            @Parameter(description = "角色名称", required = true)
            @PathVariable String name) {
        return ApiResponse.success(roleApplicationService.getRoleByName(name));
    }

    @Operation(summary = "创建角色", description = "创建新的角色")
    @PostMapping
    public ApiResponse<RoleDTO> createRole(
            @Parameter(description = "角色信息", required = true)
            @Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success(roleApplicationService.createRole(request));
    }

    @Operation(summary = "更新角色", description = "更新指定ID的角色信息")
    @PutMapping("/{id}")
    public ApiResponse<RoleDTO> updateRole(
            @Parameter(description = "角色ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "角色信息", required = true)
            @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success(roleApplicationService.updateRole(id, request));
    }

    @Operation(summary = "删除角色", description = "删除指定ID的角色")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(
            @Parameter(description = "角色ID", required = true)
            @PathVariable Long id) {
        roleApplicationService.deleteRole(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "为角色分配权限", description = "为指定角色分配权限列表")
    @PostMapping("/{roleId}/permissions")
    public ApiResponse<Void> assignPermissions(
            @Parameter(description = "角色ID", required = true)
            @PathVariable Long roleId,
            @Parameter(description = "权限ID列表", required = true)
            @Valid @RequestBody AssignPermissionsRequest request) {
        roleApplicationService.assignPermissions(roleId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取角色的权限列表", description = "返回指定角色拥有的所有权限")
    @GetMapping("/{roleId}/permissions")
    public ApiResponse<List<PermissionDTO>> getRolePermissions(
            @Parameter(description = "角色ID", required = true)
            @PathVariable Long roleId) {
        return ApiResponse.success(roleApplicationService.getRolePermissions(roleId));
    }
}


package com.huicang.wise.api.controller;

import com.huicang.wise.application.role.RoleDTO;
import com.huicang.wise.application.user.AssignRolesRequest;
import com.huicang.wise.application.user.UserRoleApplicationService;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "用户角色管理接口")
@RestController
@RequestMapping("/api/users/{userId}/roles")
public class UserRoleController {

    private final UserRoleApplicationService userRoleApplicationService;

    public UserRoleController(UserRoleApplicationService userRoleApplicationService) {
        this.userRoleApplicationService = userRoleApplicationService;
    }

    @Operation(summary = "获取用户的角色列表", description = "返回指定用户拥有的所有角色")
    @GetMapping
    public ApiResponse<List<RoleDTO>> getUserRoles(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        return ApiResponse.success(userRoleApplicationService.getUserRoles(userId));
    }

    @Operation(summary = "为用户分配角色", description = "为指定用户分配角色列表")
    @PostMapping
    public ApiResponse<Void> assignRoles(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "角色ID列表", required = true)
            @Valid @RequestBody AssignRolesRequest request) {
        userRoleApplicationService.assignRoles(userId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "移除用户的所有角色", description = "移除指定用户的所有角色")
    @DeleteMapping
    public ApiResponse<Void> removeUserRoles(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        userRoleApplicationService.removeUserRoles(userId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "移除用户的指定角色", description = "移除指定用户的指定角色")
    @DeleteMapping("/{roleId}")
    public ApiResponse<Void> removeUserRole(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "角色ID", required = true)
            @PathVariable Long roleId) {
        userRoleApplicationService.removeUserRole(userId, roleId);
        return ApiResponse.success(null);
    }
}


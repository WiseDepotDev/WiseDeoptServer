package com.huicang.wise.api.controller;

import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.common.DeleteWithVerifyRequest;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.application.user.UserCreateRequest;
import com.huicang.wise.application.user.UserDTO;
import com.huicang.wise.application.user.UserPageDTO;
import com.huicang.wise.application.user.UserPasswordChangeRequest;
import com.huicang.wise.application.user.UserUpdateRequest;
import com.huicang.wise.common.annotation.RequiresPermission;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类功能描述：用户管理控制层
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Tag(name = "用户管理接口")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserApplicationService userApplicationService;
    private final AuthApplicationService authApplicationService;

    public UserController(
            UserApplicationService userApplicationService,
            AuthApplicationService authApplicationService) {
        this.userApplicationService = userApplicationService;
        this.authApplicationService = authApplicationService;
    }

    @Operation(
            summary = "获取当前用户信息",
            description = "获取当前登录用户信息。成功返回200；未登录或Token无效返回401；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CURRENT)
    @GetMapping("/current")
    public ApiResponse<UserDTO> getCurrentUser(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = authApplicationService.validateToken(token);
        return ApiResponse.success(userApplicationService.getUserByUsername(username));
    }

    @Operation(summary = "创建用户", description = "创建用户。成功返回200；用户名已存在返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CREATE)
    @RequiresPermission("user:create")
    @PostMapping
    public ApiResponse<UserDTO> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userApplicationService.createUser(request));
    }

    @Operation(summary = "更新用户", description = "更新用户信息。成功返回200；用户不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_UPDATE)
    @RequiresPermission("user:edit")
    @PutMapping("/{userId}")
    public ApiResponse<UserDTO> updateUser(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        request.setUserId(userId);
        return ApiResponse.success(userApplicationService.updateUser(request));
    }

    @Operation(summary = "查询用户详情", description = "根据ID查询用户详情。成功返回200；用户不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_DETAIL)
    @RequiresPermission("user:view")
    @GetMapping("/{userId}")
    public ApiResponse<UserDTO> getUser(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId) {
        return ApiResponse.success(userApplicationService.getUser(userId));
    }

    @Operation(summary = "查询用户列表", description = "分页查询用户列表。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_LIST)
    @RequiresPermission("user:view")
    @GetMapping
    public ApiResponse<UserPageDTO> listUsers(
            @Parameter(description = "页码", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页数量", required = false)
                    @RequestParam(value = "size", required = false)
                    Integer size) {
        return ApiResponse.success(userApplicationService.listUsers(page, size));
    }

    /**
     * 删除用户 —— **唯一入口**，且必须带人机验证票据（校验在 Service 层）。
     *
     * <p>原先这里还有一条 `DELETE /{userId}`：**不带任何验证**，只有权限注解。于是 "删除需要验证码"只是前端那条路上的装饰 —— 任何持有
     * `user:delete` 的会话直接调它就删掉了。 孪生端点已删除，票据校验下沉到 {@code
     * UserApplicationService.deleteUserWithVerify}。
     */
    @Operation(summary = "删除用户", description = "根据ID删除用户（需要人机验证票据）。成功返回200；用户不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_DELETE)
    @RequiresPermission("user:delete")
    @PostMapping("/{userId}/delete")
    public ApiResponse<Void> deleteUserWithVerify(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId,
            @Parameter(description = "删除请求参数", required = true) @Valid @RequestBody
                    DeleteWithVerifyRequest request) {
        request.setId(userId);
        userApplicationService.deleteUserWithVerify(request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "修改密码", description = "修改用户登录密码。成功返回200；旧密码错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CHANGE_PASSWORD)
    @PostMapping("/{userId}/password")
    public ApiResponse<Void> changePassword(
            @Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId,
            @Valid @RequestBody UserPasswordChangeRequest request) {
        userApplicationService.changePassword(userId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "修改当前用户密码", description = "修改当前登录用户的登录密码。成功返回200；旧密码错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CHANGE_PASSWORD)
    @PostMapping("/current/password")
    public ApiResponse<Void> changeCurrentUserPassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserPasswordChangeRequest request) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = authApplicationService.validateToken(token);
        UserDTO user = userApplicationService.getUserByUsername(username);
        userApplicationService.changePassword(user.getUserId(), request);
        return ApiResponse.success(null);
    }
}

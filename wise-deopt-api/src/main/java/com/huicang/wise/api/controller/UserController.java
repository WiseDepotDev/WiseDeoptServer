package com.huicang.wise.api.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.application.user.UserCreateRequest;
import com.huicang.wise.application.user.UserDTO;
import com.huicang.wise.application.user.UserPageDTO;
import com.huicang.wise.application.user.UserPasswordChangeRequest;
import com.huicang.wise.application.user.UserUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 类功能描述：用户管理控制层
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Api(tags = "用户管理接口")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserApplicationService userApplicationService;
    private final AuthApplicationService authApplicationService;

    public UserController(UserApplicationService userApplicationService, AuthApplicationService authApplicationService) {
        this.userApplicationService = userApplicationService;
        this.authApplicationService = authApplicationService;
    }

    @ApiOperation(value = "获取当前用户信息", notes = "获取当前登录用户信息。成功返回200；未登录或Token无效返回401；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CURRENT)
    @GetMapping("/current")
    public ApiResponse<UserDTO> getCurrentUser(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = authApplicationService.validateToken(token);
        return ApiResponse.success(userApplicationService.getUserByUsername(username));
    }

    @ApiOperation(value = "创建用户", notes = "创建用户。成功返回200；用户名已存在返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CREATE)
    @PostMapping
    public ApiResponse<UserDTO> createUser(@RequestBody UserCreateRequest request) {
        return ApiResponse.success(userApplicationService.createUser(request));
    }

    @ApiOperation(value = "更新用户", notes = "更新用户信息。成功返回200；用户不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_UPDATE)
    @PutMapping("/{userId}")
    public ApiResponse<UserDTO> updateUser(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable("userId") Long userId,
            @RequestBody UserUpdateRequest request) {
        request.setUserId(userId);
        return ApiResponse.success(userApplicationService.updateUser(request));
    }

    @ApiOperation(value = "查询用户详情", notes = "根据ID查询用户详情。成功返回200；用户不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_DETAIL)
    @GetMapping("/{userId}")
    public ApiResponse<UserDTO> getUser(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable("userId") Long userId) {
        return ApiResponse.success(userApplicationService.getUser(userId));
    }

    @ApiOperation(value = "查询用户列表", notes = "分页查询用户列表。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_LIST)
    @GetMapping
    public ApiResponse<UserPageDTO> listUsers(
            @ApiParam(value = "页码", required = false)
            @RequestParam(value = "page", required = false) Integer page,
            @ApiParam(value = "每页数量", required = false)
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.success(userApplicationService.listUsers(page, size));
    }

    @ApiOperation(value = "删除用户", notes = "根据ID删除用户。成功返回200；用户不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_DELETE)
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable("userId") Long userId) {
        userApplicationService.deleteUser(userId);
        return ApiResponse.success(null);
    }

    @ApiOperation(value = "修改密码", notes = "修改用户登录密码。成功返回200；旧密码错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CHANGE_PASSWORD)
    @PostMapping("/{userId}/password")
    public ApiResponse<Void> changePassword(
            @ApiParam(value = "用户ID", required = true) @PathVariable("userId") Long userId,
            @RequestBody UserPasswordChangeRequest request) {
        userApplicationService.changePassword(userId, request);
        return ApiResponse.success(null);
    }
}

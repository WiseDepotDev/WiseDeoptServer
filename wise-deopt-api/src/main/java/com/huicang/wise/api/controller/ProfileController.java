package com.huicang.wise.api.controller;

import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.user.UserProfileApplicationService;
import com.huicang.wise.application.user.UserProfileDTO;
import com.huicang.wise.application.user.UserProfileUpdateRequest;
import com.huicang.wise.application.user.UserSettingsDTO;
import com.huicang.wise.application.user.UserSettingsUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 类功能描述：个人中心控制层
 *
 * @author xingchentye
 * @version 0.1.24
 * @since 2026-02-27
 */
@Tag(name = "个人中心接口")
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserProfileApplicationService userProfileApplicationService;
    private final AuthApplicationService authApplicationService;

    public ProfileController(
            UserProfileApplicationService userProfileApplicationService,
            AuthApplicationService authApplicationService) {
        this.userProfileApplicationService = userProfileApplicationService;
        this.authApplicationService = authApplicationService;
    }

    @Operation(
            summary = "获取个人资料",
            description = "获取当前登录用户的个人资料。成功返回200；未登录或Token无效返回401；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CURRENT)
    @GetMapping
    public ApiResponse<UserProfileDTO> getProfile(@RequestHeader("Authorization") String token) {
        String username = extractUsername(token);
        return ApiResponse.success(userProfileApplicationService.getUserProfile(username));
    }

    @Operation(
            summary = "更新个人资料",
            description = "更新当前登录用户的个人资料。成功返回200；未登录或Token无效返回401；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_UPDATE)
    @PutMapping
    public ApiResponse<UserProfileDTO> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UserProfileUpdateRequest request) {
        String username = extractUsername(token);
        return ApiResponse.success(
                userProfileApplicationService.updateUserProfile(username, request));
    }

    @Operation(summary = "上传头像", description = "上传并更新用户头像。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_UPDATE)
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileDTO> uploadAvatar(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file) {
        String username = extractUsername(token);
        return ApiResponse.success(userProfileApplicationService.uploadAvatar(username, file));
    }

    @Operation(summary = "删除头像", description = "删除当前用户的头像。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_UPDATE)
    @DeleteMapping("/avatar")
    public ApiResponse<UserProfileDTO> deleteAvatar(@RequestHeader("Authorization") String token) {
        String username = extractUsername(token);
        return ApiResponse.success(userProfileApplicationService.deleteAvatar(username));
    }

    @Operation(summary = "获取用户设置", description = "获取当前登录用户的设置。成功返回200；未登录或Token无效返回401；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_CURRENT)
    @GetMapping("/settings")
    public ApiResponse<UserSettingsDTO> getSettings(@RequestHeader("Authorization") String token) {
        String username = extractUsername(token);
        return ApiResponse.success(userProfileApplicationService.getUserSettings(username));
    }

    @Operation(summary = "更新用户设置", description = "更新当前登录用户的设置。成功返回200；未登录或Token无效返回401；服务器异常返回500。")
    @ApiPacketType(PacketType.USER_UPDATE)
    @PutMapping("/settings")
    public ApiResponse<UserSettingsDTO> updateSettings(
            @RequestHeader("Authorization") String token,
            @RequestBody UserSettingsUpdateRequest request) {
        String username = extractUsername(token);
        return ApiResponse.success(
                userProfileApplicationService.updateUserSettings(username, request));
    }

    @Operation(summary = "获取用户头像", description = "获取用户头像图片。需登录，只能获取自己或管理员获取任意用户头像。")
    @GetMapping(value = "/{userId}/avatar/image")
    public void getAvatarImage(
            @RequestHeader("Authorization") String token,
            @PathVariable("userId") Long userId,
            HttpServletResponse response)
            throws IOException {
        String username = extractUsername(token);
        byte[] data = userProfileApplicationService.getAvatarContent(userId, username);
        if (data == null || data.length == 0) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        StreamUtils.copy(data, response.getOutputStream());
    }

    private String extractUsername(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return authApplicationService.validateToken(token);
    }
}

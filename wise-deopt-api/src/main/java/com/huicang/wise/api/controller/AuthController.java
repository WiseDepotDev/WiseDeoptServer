package com.huicang.wise.api.controller;

import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.auth.LoginRequest;
import com.huicang.wise.application.auth.LoginResponse;
import com.huicang.wise.application.auth.RefreshTokenRequest;
import com.huicang.wise.common.annotation.RateLimit;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类功能描述：认证控制层，提供登录相关接口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现登录接口
 */
@Tag(name = "认证管理接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 方法功能描述：用户登录接口
     *
     * <p>限流是**第二道**，第一道是 `LoginAttemptGuard`（账号锁定 / IP 封禁，也是 Redis）。 两者分工：锁定管"某个账号/某台机器连续输错"，限流管"某个
     * IP 在刷接口"—— 只有锁定的话，攻击者换账号名（撞库）就能一直打，而每个账号自己都没到失败阈值。
     *
     * <p>数字刻意给得宽（比正常人手速高两个数量级）：正常用户与桥的自动重试都不会碰到它， 只有脚本会。真正要挡的是"每秒几十次"的量级。
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    @Operation(summary = "用户登录", description = "根据用户名和密码执行登录。成功返回200；认证失败返回401；服务器异常返回500。")
    @ApiPacketType(PacketType.AUTH_LOGIN)
    @RateLimit(ipLimit = 300, ipWindowSeconds = 60, apiLimit = 600, apiWindowSeconds = 60)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Parameter(description = "登录请求参数", required = true) @Valid @RequestBody
                    LoginRequest request,
            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(authApplicationService.login(request, httpServletRequest));
    }

    /**
     * 方法功能描述：NFC扫码登录接口
     *
     * @param request NFC登录请求参数
     * @return NFC登录响应结果
     */
    @Operation(summary = "NFC扫码登录", description = "根据NFC ID识别用户。成功返回用户信息和验证ID；失败返回404。")
    @ApiPacketType(PacketType.AUTH_NFC_LOGIN)
    @RateLimit(ipLimit = 300, ipWindowSeconds = 60, apiLimit = 600, apiWindowSeconds = 60)
    @PostMapping("/nfc-login")
    public ApiResponse<com.huicang.wise.application.auth.NfcLoginResponse> loginNfc(
            @Parameter(description = "NFC登录请求参数", required = true) @Valid @RequestBody
                    com.huicang.wise.application.auth.UserNfcLoginDTO request,
            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(authApplicationService.loginNfc(request, httpServletRequest));
    }

    /**
     * 方法功能描述：退出登录接口
     *
     * @param token 访问令牌
     * @return 无
     */
    @Operation(summary = "退出登录", description = "退出当前登录状态，使令牌失效。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.AUTH_LOGOUT)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authApplicationService.logout(token);
        return ApiResponse.success(null);
    }

    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌。成功返回200；令牌无效返回401。")
    @RateLimit(ipLimit = 300, ipWindowSeconds = 60, apiLimit = 600, apiWindowSeconds = 60)
    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(
            @Parameter(description = "刷新令牌请求参数", required = true) @Valid @RequestBody
                    RefreshTokenRequest request,
            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
                authApplicationService.refreshToken(request, httpServletRequest));
    }

    /**
     * 方法功能描述：NFC+PIN登录接口
     *
     * @param request NFC+PIN登录请求参数
     * @return 登录响应结果
     */
    @Operation(summary = "NFC+PIN登录", description = "根据NFC ID和PIN码执行登录。成功返回Token；失败返回对应错误码。")
    @ApiPacketType(PacketType.AUTH_NFC_PIN_LOGIN)
    @RateLimit(ipLimit = 120, ipWindowSeconds = 60, apiLimit = 300, apiWindowSeconds = 60)
    @PostMapping("/nfc-pin-login")
    public ApiResponse<LoginResponse> loginNfcPin(
            @Parameter(description = "NFC+PIN登录请求参数", required = true) @Valid @RequestBody
                    com.huicang.wise.application.auth.NfcPinDTO request,
            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(authApplicationService.nfcPinLogin(request, httpServletRequest));
    }
}

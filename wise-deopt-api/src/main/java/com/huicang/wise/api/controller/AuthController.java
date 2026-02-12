package com.huicang.wise.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.auth.LoginRequest;
import com.huicang.wise.application.auth.LoginResponse;
import com.huicang.wise.common.api.ApiResponse;

import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 类功能描述：认证控制层，提供登录相关接口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现登录接口
 */
@Api(tags = "认证管理接口")
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
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    @ApiOperation(
            value = "用户登录",
            notes = "根据用户名和密码执行登录。成功返回200；认证失败返回401；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.AUTH_LOGIN)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @ApiParam(value = "登录请求参数", required = true)
            @RequestBody LoginRequest request) {
        return ApiResponse.success(authApplicationService.login(request));
    }

    /**
     * 方法功能描述：NFC扫码登录接口
     *
     * @param request NFC登录请求参数
     * @return NFC登录响应结果
     */
    @ApiOperation(
            value = "NFC扫码登录",
            notes = "根据NFC ID识别用户。成功返回用户信息和验证ID；失败返回404。"
    )
    @ApiPacketType(PacketType.AUTH_NFC_LOGIN)
    @PostMapping("/nfc-login")
    public ApiResponse<com.huicang.wise.application.auth.NfcLoginResponse> loginNfc(
            @ApiParam(value = "NFC登录请求参数", required = true)
            @RequestBody com.huicang.wise.application.auth.UserNfcLoginDTO request) {
        return ApiResponse.success(authApplicationService.loginNfc(request));
    }

    /**
     * 方法功能描述：退出登录接口
     *
     * @param token 访问令牌
     * @return 无
     */
    @ApiOperation(
            value = "退出登录",
            notes = "退出当前登录状态，使令牌失效。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.AUTH_LOGOUT)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@org.springframework.web.bind.annotation.RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authApplicationService.logout(token);
        return ApiResponse.success(null);
    }

    /**
     * 方法功能描述：NFC+PIN登录接口
     *
     * @param request NFC+PIN登录请求参数
     * @return 登录响应结果
     */
    @ApiOperation(
            value = "NFC+PIN登录",
            notes = "根据NFC ID和PIN码执行登录。成功返回Token；失败返回对应错误码。"
    )
    @ApiPacketType(PacketType.AUTH_NFC_PIN_LOGIN)
    @PostMapping("/nfc-pin-login")
    public ApiResponse<LoginResponse> loginNfcPin(
            @ApiParam(value = "NFC+PIN登录请求参数", required = true)
            @RequestBody com.huicang.wise.application.auth.NfcPinDTO request) {
        return ApiResponse.success(authApplicationService.nfcPinLogin(request));
    }
}

package com.huicang.wise.api.controller;

import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.auth.LoginRequest;
import com.huicang.wise.application.auth.LoginResponse;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @ApiParam(value = "登录请求参数", required = true)
            @RequestBody LoginRequest request) {
        return ApiResponse.success(authApplicationService.login(request));
    }
}


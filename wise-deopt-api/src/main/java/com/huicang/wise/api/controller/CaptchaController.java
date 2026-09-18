package com.huicang.wise.api.controller;

import com.huicang.wise.application.captcha.CaptchaApplicationService;
import com.huicang.wise.application.captcha.CaptchaDTO;
import com.huicang.wise.application.captcha.CaptchaGenerateRequest;
import com.huicang.wise.application.captcha.CaptchaVerifyRequest;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "验证码管理接口")
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private final CaptchaApplicationService captchaApplicationService;

    public CaptchaController(CaptchaApplicationService captchaApplicationService) {
        this.captchaApplicationService = captchaApplicationService;
    }

    @Operation(summary = "生成验证码", description = "生成图形验证码，返回验证码ID和Base64编码的图片")
    @PostMapping("/generate")
    public ApiResponse<CaptchaDTO> generateCaptcha(
            @Parameter(description = "验证码生成请求参数", required = false)
                    @Valid
                    @RequestBody(required = false)
                    CaptchaGenerateRequest request) {
        if (request == null) {
            request = new CaptchaGenerateRequest();
        }
        CaptchaDTO captcha = captchaApplicationService.generateCaptcha(request);
        return ApiResponse.success(captcha);
    }

    @Operation(summary = "验证验证码", description = "验证图形验证码是否正确")
    @PostMapping("/verify")
    public ApiResponse<Void> verifyCaptcha(
            @Parameter(description = "验证码验证请求参数", required = true) @Valid @RequestBody
                    CaptchaVerifyRequest request) {
        captchaApplicationService.verifyCaptcha(request);
        return ApiResponse.success();
    }
}

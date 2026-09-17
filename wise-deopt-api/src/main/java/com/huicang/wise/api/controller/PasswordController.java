package com.huicang.wise.api.controller;

import com.huicang.wise.application.password.ChangePasswordRequest;
import com.huicang.wise.application.password.ForgotPasswordRequest;
import com.huicang.wise.application.password.PasswordApplicationService;
import com.huicang.wise.application.password.ResetPasswordRequest;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "密码管理接口")
@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final PasswordApplicationService passwordApplicationService;

    public PasswordController(PasswordApplicationService passwordApplicationService) {
        this.passwordApplicationService = passwordApplicationService;
    }

    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    @PostMapping("/change")
    public ApiResponse<Void> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody ChangePasswordRequest changeRequest) {
        Long userId = (Long) request.getAttribute("userId");
        passwordApplicationService.changePassword(userId, changeRequest);
        return ApiResponse.success(null);
    }

    @Operation(summary = "重置密码", description = "管理员重置用户密码")
    @PostMapping("/reset")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest resetRequest) {
        passwordApplicationService.resetPassword(resetRequest);
        return ApiResponse.success(null);
    }

    @Operation(summary = "忘记密码", description = "通过邮箱重置密码")
    @PostMapping("/forgot")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest forgotRequest) {
        passwordApplicationService.forgotPassword(forgotRequest);
        return ApiResponse.success(null);
    }

    @Operation(summary = "检查密码强度", description = "检查密码强度，返回1-5的强度等级")
    @GetMapping("/strength")
    public ApiResponse<Integer> checkPasswordStrength(
            @Parameter(description = "密码") @RequestParam String password) {
        int strength = passwordApplicationService.getPasswordStrength(password);
        return ApiResponse.success(strength);
    }
}

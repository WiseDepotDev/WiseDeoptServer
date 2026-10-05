package com.huicang.wise.api.controller;

import com.huicang.wise.application.human.HumanChallengeRequest;
import com.huicang.wise.application.human.HumanChallengeResponse;
import com.huicang.wise.application.human.HumanVerifyApplicationService;
import com.huicang.wise.application.human.HumanVerifyRequest;
import com.huicang.wise.application.human.HumanVerifyResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人机验证接口（替代已删除的图形验证码）。
 *
 * <p>两条端点都在**登录之前**调用，因此必须加入两处白名单： {@code WebMvcConfiguration}（认证拦截器）与 {@code
 * RequestSignatureFilter}（请求签名）。 漏掉任何一处，页面在登录前就拿到 401 —— 这是"新端点挂在 /api/** 下"最容易踩的坑。
 *
 * <p>两条都挂了 {@code @RateLimit}：挑战与提交都是"可以免费试"的入口，不限流就等于把 计算量证明的边际成本还回去。
 */
@Tag(name = "人机验证接口")
@RestController
@RequestMapping("/api/human")
public class HumanVerifyController {

    private final HumanVerifyApplicationService humanVerifyApplicationService;

    public HumanVerifyController(HumanVerifyApplicationService humanVerifyApplicationService) {
        this.humanVerifyApplicationService = humanVerifyApplicationService;
    }

    @Operation(summary = "申请人机验证挑战", description = "下发一次性挑战与当前难度（低/中/高三档）。风险偏高时返回冷却动作。")
    @ApiPacketType(PacketType.UNKNOWN)
    @RateLimit(ipLimit = 60, ipWindowSeconds = 60, apiLimit = 200, apiWindowSeconds = 60)
    @PostMapping("/challenge")
    public ApiResponse<HumanChallengeResponse> challenge(
            @Parameter(description = "挑战请求（用途/证据）", required = true) @Valid @RequestBody
                    HumanChallengeRequest request,
            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
                humanVerifyApplicationService.challenge(request, clientIp(httpServletRequest)));
    }

    @Operation(summary = "提交人机验证结果", description = "校验设备签名与计算量证明；通过则下发一次性票据（humanToken）。")
    @ApiPacketType(PacketType.UNKNOWN)
    @RateLimit(ipLimit = 60, ipWindowSeconds = 60, apiLimit = 200, apiWindowSeconds = 60)
    @PostMapping("/verify")
    public ApiResponse<HumanVerifyResponse> verify(
            @Parameter(description = "验证请求（签名/计算量证明）", required = true) @Valid @RequestBody
                    HumanVerifyRequest request,
            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(
                humanVerifyApplicationService.verify(request, clientIp(httpServletRequest)));
    }

    /** 与 {@code RateLimitInterceptor} 同一口径地取客户端 IP（网关转发时看 X-Forwarded-For 的第一段）。 */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}

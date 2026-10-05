package com.huicang.wise.api.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huicang.wise.common.annotation.RateLimit;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 限流**覆盖面**门禁（纯反射，不需要容器）。
 *
 * <p>守的是 2026-10-05 审计发现的"机器造好了没插电"：`RateLimiterService`（Redis + Lua 滑动窗口） 与 `RateLimitInterceptor`
 * 都在，但 `RateLimitInterceptor` 对**没有注解的方法直接放行**， 而全仓只有两个端点带 `@RateLimit`（人机验证挑战与提交）—— 也就是说
 * `/api/auth/login` 这类"最该限流的入口"当时一次限流都没有。
 *
 * <p>为什么这四条件必须限流：它们都能被**未认证**调用，且各自有一份可暴力尝试的凭据 （口令 / PIN / 刷新令牌）。`LoginAttemptGuard`
 * 只管"某个账号连续失败"，撞库（换账号名） 只有限流能挡。
 */
class RateLimitCoverageTest {

    /** 端点 → 它可被暴力尝试的东西。 */
    private static final List<String> MUST_BE_RATE_LIMITED =
            List.of("login", "refreshToken", "loginNfc", "loginNfcPin");

    @Test
    @DisplayName("四条认证入口都必须挂 @RateLimit（未认证可达 + 有可爆破的凭据）")
    void authEntrypointsAreRateLimited() {
        for (String methodName : MUST_BE_RATE_LIMITED) {
            Method method = findMethod(AuthController.class, methodName);
            assertTrue(
                    method.getAnnotation(RateLimit.class) != null,
                    "AuthController."
                            + methodName
                            + " 没有 @RateLimit —— RateLimitInterceptor 对没有注解的方法直接放行，"
                            + "等于这条入口不限流（撞库只靠账号锁定挡不住）。");
        }
    }

    @Test
    @DisplayName("限流数字必须是「挡脚本、不挡人」的量级（不是把正常用户挡在外面）")
    void limitsAreSane() {
        for (String methodName : MUST_BE_RATE_LIMITED) {
            RateLimit limit =
                    findMethod(AuthController.class, methodName).getAnnotation(RateLimit.class);
            assertTrue(
                    limit.ipLimit() >= 60,
                    "AuthController."
                            + methodName
                            + " 的 IP 限额太小（"
                            + limit.ipLimit()
                            + "/min）："
                            + "一台工位后面可能坐着一个团队，限太小会伤人而不是限脚本");
            assertTrue(
                    limit.apiLimit() >= limit.ipLimit(),
                    "AuthController." + methodName + " 的接口级限额不应小于 IP 级（两者互为兜底）");
        }
    }

    private Method findMethod(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(type.getSimpleName() + " 里没有 " + name));
    }
}

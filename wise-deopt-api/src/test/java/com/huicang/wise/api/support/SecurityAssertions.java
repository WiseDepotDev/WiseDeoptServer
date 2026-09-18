package com.huicang.wise.api.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * 安全类测试的共用断言（STD-TEST-01）。
 *
 * <p>背景：注入类测试（SQL 注入 / XSS）此前的写法是 `status().isOk()`，即把 「服务端接受了恶意输入并正常处理」当作预期。但自 P2-08 起控制器全面启用
 * `@Valid`， 超出 `@Size`/`@Pattern` 约束的注入载荷会被参数校验以 400 拒绝——**这同样是安全结果**， 甚至更早地阻断在入口。
 *
 * <p>因此断言应从「恰好 200」改为**真正的不变量**：
 *
 * <ol>
 *   <li>恶意输入不得导致 5xx（服务端不得因注入而异常）；
 *   <li>响应中不得原样回显注入标记（不得把载荷当作内容回吐）。
 * </ol>
 *
 * 这样无论参数校验是否拦截（200 或 400），断言都成立且更有意义。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
public final class SecurityAssertions {

    /** 常见的注入标记，出现于响应体即视为回显 */
    private static final List<String> INJECTION_MARKERS =
            List.of(
                    "<script",
                    "<img",
                    "<iframe",
                    "<svg",
                    "onerror=",
                    "onclick=",
                    "javascript:",
                    "UNION SELECT",
                    "OR '1'='1",
                    "SLEEP(");

    private SecurityAssertions() {}

    /**
     * 断言：不因注入输入产生 5xx，且响应体不回显注入标记。
     *
     * @return MockMvc 断言器
     */
    public static ResultMatcher noServerErrorAndNoEcho() {
        return result -> {
            int status = result.getResponse().getStatus();
            assertTrue(status < 500, "注入输入不得导致服务端异常（5xx），实际状态=" + status);

            String body = result.getResponse().getContentAsString();
            if (body == null || body.isEmpty()) {
                return;
            }
            String lower = body.toLowerCase();
            for (String marker : INJECTION_MARKERS) {
                assertFalse(
                        lower.contains(marker.toLowerCase()),
                        "响应不得原样回显注入标记 ["
                                + marker
                                + "]，响应片段="
                                + body.substring(0, Math.min(200, body.length())));
            }
        };
    }
}

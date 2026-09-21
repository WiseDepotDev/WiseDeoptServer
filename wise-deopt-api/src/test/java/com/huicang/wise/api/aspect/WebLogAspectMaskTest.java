package com.huicang.wise.api.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * WebLogAspect 日志脱敏用例（P5-06 / M-17）。
 *
 * <p>这条门禁要钉死的是"**口令与令牌绝不进日志**"：只要 {@link WebLogAspect#maskSensitive(String)}
 * 的返回值里还能看到明文口令/JWT，测试就必须红。同时验证脱敏不会破坏 JSON 结构与其余字段。
 */
class WebLogAspectMaskTest {

    private static final String JWT =
            "eyJhbGciOiJIUzUxMiJ9.eyJ0eXBlIjoiYWNjZXNzIiwidXNlcklkIjoxfQ.RdncgzDFDuQAEqe0Dz1FDI";

    @Test
    @DisplayName("登录请求：password 被掩码，username 原样保留")
    void masksLoginPassword() {
        String masked = WebLogAspect.maskSensitive(
                "{\"username\":\"admin\",\"password\":\"Wise-Admin-2026!\"}");

        assertFalse(masked.contains("Wise-Admin-2026!"), "明文口令不得出现在日志文本里");
        assertTrue(masked.contains("\"password\":\"***\""), "password 字段应被替换为 ***");
        assertTrue(masked.contains("\"username\":\"admin\""), "非敏感字段不得被改动");
    }

    @Test
    @DisplayName("登录响应：token / refreshToken 均被掩码")
    void masksTokensInResponse() {
        String masked = WebLogAspect.maskSensitive(
                "{\"code\":\"RES-0000\",\"data\":{\"token\":\"" + JWT + "\",\"refreshToken\":\"" + JWT + "\"}}");

        assertFalse(masked.contains(JWT), "JWT 不得出现在日志文本里");
        assertTrue(masked.contains("\"token\":\"***\""));
        assertTrue(masked.contains("\"refreshToken\":\"***\""));
        assertTrue(masked.contains("\"code\":\"RES-0000\""), "业务码不受影响");
    }

    @Test
    @DisplayName("未知字段名下的 JWT 也要按值形态兜底掩码")
    void masksJwtEvenUnderUnknownKey() {
        String masked = WebLogAspect.maskSensitive("{\"credential\":\"" + JWT + "\"}");

        assertFalse(masked.contains(JWT));
        assertTrue(masked.contains("\"***\""));
    }

    @Test
    @DisplayName("字段名大小写不敏感，且覆盖 secret/signature/apiKey/pin/captcha")
    void masksByFieldNameCaseInsensitively() {
        String masked = WebLogAspect.maskSensitive(
                "{\"Password\":\"p1\",\"signatureSecret\":\"s1\",\"X-Signature\":\"s2\","
                        + "\"apiKey\":\"k1\",\"pin\":\"1234\",\"captcha\":\"9876\"}");

        for (String plain : new String[]{"p1", "s1", "s2", "k1", "1234", "9876"}) {
            assertFalse(masked.contains("\"" + plain + "\""), "值 " + plain + " 不应以明文出现");
        }
    }

    @Test
    @DisplayName("无敏感内容时保持原样；null 与空串安全")
    void passesThroughHarmlessContent() {
        String plain = "{\"deviceCode\":\"wd-001\",\"status\":1}";
        assertEquals(plain, WebLogAspect.maskSensitive(plain));
        assertNull(WebLogAspect.maskSensitive(null));
        assertEquals("", WebLogAspect.maskSensitive(""));
    }

    @Test
    @DisplayName("幂等：对已脱敏文本再脱敏不产生变化")
    void isIdempotent() {
        String once = WebLogAspect.maskSensitive("{\"password\":\"secret\",\"token\":\"" + JWT + "\"}");
        assertEquals(once, WebLogAspect.maskSensitive(once));
    }
}

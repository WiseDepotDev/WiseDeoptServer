package com.huicang.wise.infrastructure.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * {@link RequestLoggingFilter} 的日志安全与链路标识单元测试（STD-LOG-01 / STD-LOG-02）。
 *
 * <p>锁定两条容易回退的行为：
 * <ul>
 *   <li>敏感请求头与报文中的敏感字段**必须脱敏**，不得明文落日志；</li>
 *   <li>MDC {@code request_id} 必须在请求处理期间存在、请求结束后清除（避免线程复用串号）。</li>
 * </ul>
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class RequestLoggingFilterTest {

    /**
     * 敏感请求头必须整值掩码。
     */
    @Test
    @DisplayName("shouldMaskSensitiveHeaders")
    void shouldMaskSensitiveHeaders() {
        assertEquals("***", RequestLoggingFilter.maskHeader("Authorization", "Bearer abc.def"));
        assertEquals("***", RequestLoggingFilter.maskHeader("authorization", "Bearer abc.def"));
        assertEquals("***", RequestLoggingFilter.maskHeader("X-Access-Secret", "s3cr3t"));
        assertEquals("***", RequestLoggingFilter.maskHeader("X-Access-Key", "ak-1"));
        assertEquals("***", RequestLoggingFilter.maskHeader("Cookie", "sid=1"));
        assertEquals("***", RequestLoggingFilter.maskHeader("Set-Cookie", "sid=1"));
        assertEquals("***", RequestLoggingFilter.maskHeader("X-Signature", "sig"));
    }

    /**
     * 非敏感请求头保持原样。
     */
    @Test
    @DisplayName("shouldKeepNonSensitiveHeaders")
    void shouldKeepNonSensitiveHeaders() {
        assertEquals("application/json", RequestLoggingFilter.maskHeader("Content-Type", "application/json"));
        assertEquals("req-1", RequestLoggingFilter.maskHeader("REQUEST-ID", "req-1"));
        assertNull(RequestLoggingFilter.maskHeader(null, null));
    }

    /**
     * 报文中的敏感字段必须脱敏，非敏感字段保留。
     */
    @Test
    @DisplayName("shouldRedactSensitiveJsonFields")
    void shouldRedactSensitiveJsonFields() {
        String body = "{\"username\":\"admin\",\"password\":\"p@ssw0rd\",\"refreshToken\":\"rt-1\",\"pin\":\"1234\"}";
        String redacted = RequestLoggingFilter.redactSensitive(body);

        assertTrue(redacted.contains("\"password\":\"***\""), redacted);
        assertTrue(redacted.contains("\"refreshToken\":\"***\""), redacted);
        assertTrue(redacted.contains("\"pin\":\"***\""), redacted);
        assertTrue(redacted.contains("\"username\":\"admin\""), "非敏感字段应保留: " + redacted);
        assertTrue(!redacted.contains("p@ssw0rd"), "不得出现明文口令: " + redacted);
    }

    /**
     * 超长报文应被截断，避免日志膨胀。
     */
    @Test
    @DisplayName("shouldTruncateLongBody")
    void shouldTruncateLongBody() {
        String body = "x".repeat(5000);
        String redacted = RequestLoggingFilter.redactSensitive(body);

        assertTrue(redacted.length() < 5000, "应被截断");
        assertTrue(redacted.endsWith("... (截断)"), redacted.substring(Math.max(0, redacted.length() - 20)));
    }

    /**
     * 空报文应安全返回。
     */
    @Test
    @DisplayName("shouldHandleEmptyBody")
    void shouldHandleEmptyBody() {
        assertNull(RequestLoggingFilter.redactSensitive(null));
        assertEquals("", RequestLoggingFilter.redactSensitive(""));
    }

    /**
     * 请求处理期间 MDC 应带有链路标识，结束后必须清除。
     *
     * @throws Exception 过滤器执行异常
     */
    @Test
    @DisplayName("shouldSetAndClearRequestIdInMdc")
    void shouldSetAndClearRequestIdInMdc() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("REQUEST-ID", "req-abc-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] duringRequest = new String[1];
        FilterChain chain = (req, res) -> duringRequest[0] = MDC.get(RequestLoggingFilter.MDC_REQUEST_ID);

        MDC.clear();
        filter.doFilter(request, response, chain);

        assertEquals("req-abc-1", duringRequest[0], "处理期间 MDC 应带 request_id");
        assertNull(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID), "请求结束后必须清除 MDC");
        assertEquals("req-abc-1", request.getAttribute(RequestLoggingFilter.ATTRIBUTE_REQUEST_ID));
    }

    /**
     * 未携带链路标识时应生成，并在请求属性中暴露同一值（供响应包装器复用）。
     *
     * @throws Exception 过滤器执行异常
     */
    @Test
    @DisplayName("shouldGenerateRequestIdWhenHeaderMissing")
    void shouldGenerateRequestIdWhenHeaderMissing() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] duringRequest = new String[1];
        FilterChain chain = (req, res) -> duringRequest[0] = MDC.get(RequestLoggingFilter.MDC_REQUEST_ID);

        MDC.clear();
        filter.doFilter(request, response, chain);

        assertTrue(duringRequest[0] != null && !duringRequest[0].isBlank(), "应生成 request_id");
        assertEquals(duringRequest[0], request.getAttribute(RequestLoggingFilter.ATTRIBUTE_REQUEST_ID));
        assertNull(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID), "请求结束后必须清除 MDC");
    }
}

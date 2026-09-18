package com.huicang.wise.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 请求日志过滤器：注入链路标识（MDC）并记录请求/响应摘要。
 *
 * <p>依据 STD-LOG-01 / STD-LOG-02：
 * <ul>
 *   <li>每个请求写入 MDC {@code request_id}，使同一次请求的所有日志可串联；</li>
 *   <li><b>敏感信息必须掩码</b>：请求头中的 Authorization / X-Access-Secret / Cookie 等，
 *       以及请求体与响应体中的 password / pin / token / secret 等字段一律脱敏；</li>
 *   <li>明细（含请求体/响应体）降为 {@code DEBUG}，生产（INFO）只记一行摘要。</li>
 * </ul>
 *
 * <p>该过滤器以最高优先级注册，确保后续所有组件的日志都能带上 {@code request_id}。
 *
 * @author WiseDepot
 * @version 1.1
 * @since 2026-02-27
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /** MDC 中的链路标识键，与统一标准 STD-LOG-01 一致 */
    public static final String MDC_REQUEST_ID = "request_id";

    /** 请求属性名：供响应包装器复用同一个链路标识 */
    public static final String ATTRIBUTE_REQUEST_ID = "REQUEST_ID_RESOLVED";

    private static final String REQUEST_ID_HEADER = "REQUEST-ID";

    /** 历史下划线写法，仅作兼容读取 */
    private static final String REQUEST_ID_HEADER_LEGACY = "REQUEST_ID";

    /** 需要整值掩码的请求头（小写比较） */
    private static final Set<String> MASKED_HEADERS = Set.of(
            "authorization",
            "proxy-authorization",
            "x-access-key",
            "x-access-secret",
            "x-signature",
            "cookie",
            "set-cookie");

    /** 报文长度上限，超出截断，避免日志膨胀 */
    private static final int MAX_BODY_LOG_LENGTH = 2000;

    /** 敏感字段脱敏：JSON 字符串值整体替换为 *** */
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword|pin|token|accessToken|refreshToken|secretKey|secret|"
                    + "accessSecret|signature|verificationId|captchaCode)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    /**
     * 请求头脱敏。
     *
     * @param name  头名称
     * @param value 头值
     * @return 敏感头返回 {@code ***}，其余原样返回
     */
    static String maskHeader(String name, String value) {
        if (name == null) {
            return value;
        }
        if (MASKED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            return "***";
        }
        return value;
    }

    /**
     * 报文脱敏并截断。
     *
     * @param body 原始报文（可为 null）
     * @return 脱敏并截断后的报文
     */
    static String redactSensitive(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        String redacted = SENSITIVE_JSON_FIELD.matcher(body).replaceAll("$1\"***\"");
        if (redacted.length() > MAX_BODY_LOG_LENGTH) {
            redacted = redacted.substring(0, MAX_BODY_LOG_LENGTH) + "... (截断)";
        }
        return redacted;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestId = resolveRequestId(request);
        MDC.put(MDC_REQUEST_ID, requestId);
        request.setAttribute(ATTRIBUTE_REQUEST_ID, requestId);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // 生产只保留一行摘要；明细走 DEBUG
            log.info("{} {} -> {} ({} ms)", wrappedRequest.getMethod(),
                    wrappedRequest.getRequestURI(), wrappedResponse.getStatus(), duration);
            if (log.isDebugEnabled()) {
                log.debug(buildDetail(wrappedRequest, wrappedResponse, duration, requestId));
            }
            wrappedResponse.copyBodyToResponse();
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    /**
     * 解析链路标识：优先请求头（REQUEST-ID，兼容 REQUEST_ID），缺失时生成。
     *
     * @param request 当前请求
     * @return 链路标识
     */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader(REQUEST_ID_HEADER_LEGACY);
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    /**
     * 组装请求明细（已脱敏），仅在 DEBUG 级别使用。
     *
     * @param request  包装后的请求
     * @param response 包装后的响应
     * @param duration 耗时毫秒
     * @param requestId 链路标识
     * @return 明细文本
     */
    private String buildDetail(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
                               long duration, String requestId) {
        StringBuilder detail = new StringBuilder();
        detail.append("\n============ 请求明细 (request_id=").append(requestId).append(") ============\n");
        detail.append(String.format("请求方法: %s%n", request.getMethod()));
        detail.append(String.format("请求URI: %s%n", request.getRequestURI()));
        detail.append(String.format("查询字符串: %s%n", request.getQueryString()));
        detail.append(String.format("远程地址: %s%n", request.getRemoteAddr()));
        detail.append(String.format("请求耗时: %d ms%n", duration));

        detail.append("请求头:\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            detail.append(String.format("  %s: %s%n", headerName, maskHeader(headerName, request.getHeader(headerName))));
        }

        detail.append("请求参数:\n");
        var parameterMap = request.getParameterMap();
        if (parameterMap.isEmpty()) {
            detail.append("  无\n");
        } else {
            parameterMap.forEach((key, values) -> detail.append(String.format("  %s: %s%n",
                    key, redactSensitive(String.join(", ", values)))));
        }

        detail.append("请求体:\n").append(formatBody(request.getContentAsByteArray(), request.getContentType()));
        detail.append(String.format("响应状态: %d%n", response.getStatus()));
        detail.append("响应体:\n").append(formatBody(response.getContentAsByteArray(), response.getContentType()));
        detail.append("============ 请求明细结束 ============\n");
        return detail.toString();
    }

    /**
     * 格式化报文体：二进制只记类型与长度，文本做脱敏与截断。
     *
     * @param content     字节内容
     * @param contentType 内容类型
     * @return 可读文本
     */
    private String formatBody(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            return "  无\n";
        }
        if (contentType != null && (contentType.startsWith("image/")
                || contentType.startsWith("multipart/form-data")
                || MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType))) {
            return String.format("  [二进制内容: %s, %d bytes]%n", contentType, content.length);
        }
        return String.format("  %s%n", redactSensitive(new String(content, StandardCharsets.UTF_8)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator")
                || path.contains("/swagger")
                || path.contains("/api-docs")
                || path.contains("/webjars")
                || path.contains("/favicon.ico")
                || path.equals("/api/device/heartbeat");
    }
}

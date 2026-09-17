package com.huicang.wise.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RequestSignatureFilter extends OncePerRequestFilter {

    @Autowired
    private RequestSignatureService requestSignatureService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String NONCE_PREFIX = "api:nonce:";
    private static final long NONCE_EXPIRE_SECONDS = 300;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (isExcludedPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String signature = request.getHeader("X-Signature");
            String timestamp = request.getHeader("X-Timestamp");
            String nonce = request.getHeader("X-Nonce");

            if (signature == null || timestamp == null || nonce == null) {
                log.warn("Missing signature headers: Signature={}, Timestamp={}, Nonce={}, URI={}", signature, timestamp, nonce, uri);
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "缺少必要的签名参数");
                return;
            }

            if (!requestSignatureService.validateTimestamp(timestamp)) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "请求时间戳无效");
                return;
            }

            String nonceKey = NONCE_PREFIX + nonce;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(nonceKey))) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "请求已重复");
                return;
            }

            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });

            log.info("Signature verification - Method: {}, URI: {}, Params: {}, Timestamp: {}, Nonce: {}", method, uri, params, timestamp, nonce);

            if (!requestSignatureService.verifySignature(method, uri, params, timestamp, nonce, signature)) {
                log.error("Signature verification failed for URI: {}", uri);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "签名验证失败");
                return;
            }

            redisTemplate.opsForValue().set(nonceKey, "1", NONCE_EXPIRE_SECONDS, TimeUnit.SECONDS);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("请求签名验证异常", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "签名验证异常");
        }
    }

    private boolean isExcludedPath(String uri) {
        return uri.startsWith("/api/auth/login") ||
               uri.startsWith("/api/auth/register") ||
               uri.startsWith("/api/auth/nfc-login") ||
               uri.startsWith("/api/auth/nfc-pin-login") ||
               uri.startsWith("/api/captcha") ||
               uri.startsWith("/api-docs") ||
               uri.startsWith("/v3/api-docs") ||
               uri.startsWith("/swagger") ||
               uri.startsWith("/swagger-ui") ||
               uri.startsWith("/swagger-resources") ||
               uri.startsWith("/webjars") ||
               uri.startsWith("/actuator") ||
               uri.startsWith("/error") ||
               uri.startsWith("/api/inventory/report") ||
               uri.startsWith("/api/rfid/report") ||
               uri.startsWith("/api/device") ||
               uri.startsWith("/device") ||
               uri.startsWith("/api/inspection") ||
               uri.startsWith("/api/inventories/all");
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        log.warn("RequestSignatureFilter Error: status={}, message={}", status, message);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}

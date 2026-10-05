package com.huicang.wise.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 请求签名过滤器的**防重放**语义测试（不需要真 Redis）。
 *
 * <p>守的是 2026-10-05 审计发现的一个 TOCTOU：原来是"先 `hasKey` 判重、验完签名再 `set` 占位"， 两步之间同一个签名请求并发发两次可以**双双通过** ——
 * 而"重放"正是签名机制要挡的那件事 （设备端已经按 `crypto.h` 的注释生成了密码学随机的 nonce，服务端这一半必须真的原子）。
 *
 * <p>断言方式是"看它发了什么命令"：占位必须是 `setIfAbsent`（`SET NX EX` 一步完成）， 而且**不许**再出现 `hasKey` 那种"先问后写"的形状。
 */
class RequestSignatureFilterTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    private final RequestSignatureService signatureService = mock(RequestSignatureService.class);

    private RequestSignatureFilter filter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        filter = new RequestSignatureFilter();
        ReflectionTestUtils.setField(filter, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(filter, "requestSignatureService", signatureService);
    }

    @Test
    @DisplayName("nonce 首次出现 ⇒ 放行，并用 setIfAbsent 一步占位（带 TTL）")
    void firstNonceIsAcceptedAndReservedAtomically() throws Exception {
        givenValidSignature();
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(signedRequest(), new MockHttpServletResponse(), chain);

        verify(valueOperations).setIfAbsent("api:nonce:nonce-1", "1", Duration.ofSeconds(300));
        verify(chain).doFilter(any(), any());
        // "先问后写"的形状不许复活：它给了并发重放一个窗口
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("nonce 已被占用（并发/重放）⇒ 400 拒绝，且**不**继续往下走")
    void replayedNonceIsRejected() throws Exception {
        givenValidSignature();
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(signedRequest(), response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(
                String.valueOf(response.getContentAsString()).contains("重复"),
                "响应要说清是「重复请求」，实际：" + response.getContentAsString());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("登录等白名单路径不参与防重放（设备端拿不到签名能力的那几条）")
    void excludedPathSkipsNonceStore() throws Exception {
        MockHttpServletRequest request = signedRequest();
        request.setRequestURI("/api/auth/login");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("带 Bearer 的请求走认证链路，不要求签名头（桥就是这么发的）")
    void bearerRequestSkipsSignature() throws Exception {
        MockHttpServletRequest request = signedRequest();
        request.addHeader("Authorization", "Bearer x");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    private void givenValidSignature() {
        when(signatureService.validateTimestamp(anyString())).thenReturn(true);
        when(signatureService.verifySignature(
                        anyString(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    private MockHttpServletRequest signedRequest() {
        // 路径要选一个**不在**签名白名单里的：`/api/device`、`/api/inspection` 等都在白名单里，
        // 拿它们当样本会得到"过滤器根本没参与"的假绿
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/1");
        request.addHeader("X-Signature", "sig");
        request.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        request.addHeader("X-Nonce", "nonce-1");
        return request;
    }
}

package com.huicang.wise.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * {@link GlobalRequestAdvice} 的信封口径单测（决策 3：三端只支持最新形状）。
 *
 * <p>锁两条行为：
 *
 * <ul>
 *   <li><b>只处理 JSON 体</b>：Jackson 转换器 → 介入；String 转换器（设备日志 {@code text/plain}）→ 放行原样绑定。
 *       后者修掉了"纯文本日志上传必然 400"的既有缺陷。
 *   <li><b>只接受统一信封</b>：扁平体 / 半个信封一律拒绝（{@link HttpMessageNotReadableException} → 400 + {@code
 *       VAL-REQUEST-1001}）；信封则只把 {@code payload.data} 交给控制器。
 * </ul>
 */
class GlobalRequestAdviceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GlobalRequestAdvice advice = new GlobalRequestAdvice();

    @BeforeEach
    void setUp() throws Exception {
        Field f = GlobalRequestAdvice.class.getDeclaredField("objectMapper");
        f.setAccessible(true);
        f.set(advice, objectMapper);
    }

    private static HttpInputMessage message(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
    }

    private String unwrapped(String body) throws IOException {
        HttpInputMessage out =
                advice.beforeBodyRead(
                        message(body), null, null, MappingJackson2HttpMessageConverter.class);
        return new String(out.getBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("只对 JSON 绑定生效：Jackson 转换器介入，String 转换器（日志 text/plain）放行")
    void supportsOnlyJsonConverter() {
        assertTrue(advice.supports(null, null, MappingJackson2HttpMessageConverter.class));
        assertFalse(advice.supports(null, null, StringHttpMessageConverter.class));
    }

    @Test
    @DisplayName("标准信封：只把 payload.data 交给控制器")
    void unwrapsEnvelopeData() throws IOException {
        String envelope =
                "{\"header\":{\"request_id\":\"r-1\",\"packet_type\":\"AUTH_LOGIN\",\"timestamp\":1},"
                        + "\"payload\":{\"code\":\"RES-0000\",\"message\":\"请求\","
                        + "\"data\":{\"username\":\"alice\",\"password\":\"secret\"}}}";

        String body = unwrapped(envelope);

        assertEquals("alice", objectMapper.readTree(body).get("username").asText());
        assertEquals("secret", objectMapper.readTree(body).get("password").asText());
    }

    @Test
    @DisplayName("信封里 data 缺失或为 null：绑定空对象")
    void unwrapsMissingDataToEmptyObject() throws IOException {
        assertEquals("{}", unwrapped("{\"header\":{},\"payload\":{\"code\":\"RES-0000\"}}"));
        assertEquals(
                "{}",
                unwrapped("{\"header\":{},\"payload\":{\"code\":\"RES-0000\",\"data\":null}}"));
    }

    @Test
    @DisplayName("扁平请求体：拒绝（不再有过渡期兼容分支）")
    void rejectsFlatBody() {
        assertThrows(
                HttpMessageNotReadableException.class,
                () -> unwrapped("{\"username\":\"alice\",\"password\":\"secret\"}"));
    }

    @Test
    @DisplayName("半个信封 / 非 JSON：同样拒绝")
    void rejectsHalfEnvelopeAndBrokenJson() {
        assertThrows(
                HttpMessageNotReadableException.class,
                () -> unwrapped("{\"header\":{\"request_id\":\"r\"}}"));
        assertThrows(
                HttpMessageNotReadableException.class,
                () -> unwrapped("{\"payload\":{\"code\":\"RES-0000\"}}"));
        assertThrows(
                HttpMessageNotReadableException.class,
                () -> unwrapped("{\"header\":\"x\",\"payload\":{}}"));
        assertThrows(HttpMessageNotReadableException.class, () -> unwrapped("{not json"));
    }
}

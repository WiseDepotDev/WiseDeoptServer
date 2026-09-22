package com.huicang.wise.api.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 全局请求解包处理器（统一信封的解包入口，STD-CONTRACT-01 / STD-CONTRACT-02）。
 *
 * <p>2026-02-27（决策 3「三端只支持最新形状」）之后的行为：
 *
 * <ul>
 *   <li><b>只处理 JSON 请求体</b>：仅当绑定用的是 Jackson 转换器时才介入。非 JSON 体
 *       （设备日志上传的 {@code text/plain}、文件上传的 {@code multipart/form-data}）由各自的绑定器处理，
 *       不再被当成信封解析——顺带修掉了"纯文本日志上传必然 400（VAL-REQUEST-1001）"的既有缺陷。
 *   <li><b>只接受统一信封</b>：根节点必须同时含 {@code header} 与 {@code payload} 对象，
 *       否则直接按"请求体不可读"拒绝（映射为 400 + {@code VAL-REQUEST-1001}）。
 *       过渡期的"无信封扁平体兼容分支"与 {@code deprecated=true} 日志已按决策 3 删除。
 *   <li>解包后只把 {@code payload.data} 交给控制器绑定（业务代码见不到信封）。
 * </ul>
 */
@RestControllerAdvice
public class GlobalRequestAdvice extends RequestBodyAdviceAdapter {

    @Autowired private ObjectMapper objectMapper;

    /**
     * 只对 JSON 请求体生效。
     *
     * <p>判据用"绑定用了哪个转换器"而不是 Content-Type 字符串：Jackson 转换器即 JSON 绑定；
     * String/表单/多部分等一律放行原样绑定。
     */
    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        return new PacketUnwrappingInputMessage(inputMessage, objectMapper);
    }

    private static class PacketUnwrappingInputMessage implements HttpInputMessage {
        private final HttpInputMessage originalMessage;
        private final InputStream body;

        PacketUnwrappingInputMessage(HttpInputMessage originalMessage, ObjectMapper objectMapper)
                throws IOException {
            this.originalMessage = originalMessage;

            JsonNode rootNode;
            try {
                rootNode = objectMapper.readTree(originalMessage.getBody());
            } catch (Exception e) {
                throw new HttpMessageNotReadableException("JSON parse error: " + e.getMessage(), originalMessage);
            }

            if (rootNode == null) {
                this.body = new ByteArrayInputStream(new byte[0]);
                return;
            }

            JsonNode headerNode = rootNode.path("header");
            JsonNode payloadNode = rootNode.path("payload");

            // 决策 3：只支持最新形状。没有统一信封的请求体一律拒绝，不再走过渡期兼容分支。
            if (!headerNode.isObject() || !payloadNode.isObject()) {
                throw new HttpMessageNotReadableException(
                        "请求体缺少统一信封（header/payload）：请按 STD-CONTRACT-01 封装后再请求",
                        originalMessage);
            }

            JsonNode dataNode = payloadNode.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                this.body = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
            } else {
                this.body = new ByteArrayInputStream(dataNode.toString().getBytes(StandardCharsets.UTF_8));
            }
        }

        @Override
        public InputStream getBody() throws IOException {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return originalMessage.getHeaders();
        }
    }
}

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
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 全局请求解包处理器
 *
 * @author xingchentye
 * @version 1.1
 * @since 2026-02-27
 */
@RestControllerAdvice
public class GlobalRequestAdvice extends RequestBodyAdviceAdapter {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return new PacketUnwrappingInputMessage(inputMessage, objectMapper);
    }

    private static class PacketUnwrappingInputMessage implements HttpInputMessage {
        private final HttpInputMessage originalMessage;
        private final InputStream body;

        public PacketUnwrappingInputMessage(HttpInputMessage originalMessage, ObjectMapper objectMapper) throws IOException {
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

            if (headerNode.isMissingNode() && payloadNode.isMissingNode()) {
                this.body = new ByteArrayInputStream(rootNode.toString().getBytes(StandardCharsets.UTF_8));
                return;
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

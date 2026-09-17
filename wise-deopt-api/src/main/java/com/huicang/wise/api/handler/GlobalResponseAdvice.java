package com.huicang.wise.api.handler;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.Packet;
import com.huicang.wise.common.protocol.PacketHeader;
import com.huicang.wise.common.protocol.PacketType;

/**
 * 全局响应包装处理器
 *
 * @author xingchentye
 * @version 1.1
 * @since 2026-02-27
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<Method, String> packetTypeCache = new ConcurrentHashMap<>();

    private static final String REQUEST_ID_ATTRIBUTE = "REQUEST_ID";
    private static final String PACKET_TYPE_ATTRIBUTE = "PACKET_TYPE";
    private static final String TIMESTAMP_ATTRIBUTE = "TIMESTAMP";
    private static final String REQUEST_START_TIME_ATTRIBUTE = "REQUEST_START_TIME";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        String className = returnType.getDeclaringClass().getName();
        return !className.contains("springdoc") && !className.contains("swagger");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Packet) {
            return body;
        }

        String requestId = (String) request.getHeaders().getFirst(REQUEST_ID_ATTRIBUTE);
        String packetTypeCode = (String) request.getHeaders().getFirst(PACKET_TYPE_ATTRIBUTE);
        Long timestamp = request.getHeaders().getFirst(TIMESTAMP_ATTRIBUTE) != null 
            ? Long.parseLong(request.getHeaders().getFirst(TIMESTAMP_ATTRIBUTE)) 
            : System.currentTimeMillis();
        Long requestStartTime = request.getHeaders().getFirst(REQUEST_START_TIME_ATTRIBUTE) != null
            ? Long.parseLong(request.getHeaders().getFirst(REQUEST_START_TIME_ATTRIBUTE))
            : System.currentTimeMillis();

        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        if (packetTypeCode == null) {
            packetTypeCode = getPacketTypeCode(returnType);
        }

        PacketHeader header = new PacketHeader();
        header.setRequestId(requestId);
        header.setPacketType(packetTypeCode);
        header.setTimestamp(timestamp);

        Packet<Object> packet = new Packet<>(header, body);

        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(packet);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("序列化响应失败", e);
            }
        }

        return packet;
    }

    private String getPacketTypeCode(MethodParameter returnType) {
        Method method = returnType.getMethod();
        if (method == null) {
            return PacketType.UNKNOWN.getCode();
        }

        return packetTypeCache.computeIfAbsent(method, m -> {
            ApiPacketType apiPacketType = m.getAnnotation(ApiPacketType.class);
            if (apiPacketType != null) {
                return apiPacketType.value().getCode();
            }
            apiPacketType = m.getDeclaringClass().getAnnotation(ApiPacketType.class);
            if (apiPacketType != null) {
                return apiPacketType.value().getCode();
            }
            return PacketType.UNKNOWN.getCode();
        });
    }
}

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
import com.huicang.wise.common.protocol.PacketBody;
import com.huicang.wise.common.protocol.PacketHeader;
import com.huicang.wise.common.protocol.PacketType;

/**
 * 全局响应包装处理器
 *
 * @author xingchentye
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<Method, String> packetTypeCache = new ConcurrentHashMap<>();

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 排除Swagger相关的接口，避免影响文档生成
        String className = returnType.getDeclaringClass().getName();
        return !className.contains("springdoc") && !className.contains("swagger");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 如果已经是Packet类型，不再包装
        if (body instanceof Packet) {
            return body;
        }

        // 获取PacketType
        String packetTypeCode = getPacketTypeCode(returnType);
        
        // 如果是未知类型且响应体是失败的ApiResponse，尝试使用SYSTEM_ERROR
        if (PacketType.UNKNOWN.getCode().equals(packetTypeCode) && body instanceof ApiResponse) {
            ApiResponse<?> apiResponse = (ApiResponse<?>) body;
            if (!apiResponse.isSuccess()) {
                packetTypeCode = PacketType.SYSTEM_ERROR.getCode();
            }
        }

        // 构造Header
        PacketHeader header = new PacketHeader();
        header.setPacketId(UUID.randomUUID().toString());
        header.setPacketType(packetTypeCode);
        header.setDirection("RESPONSE");
        header.setTimestamp(System.currentTimeMillis());
        header.setVersion("1.0");

        // 构造Body
        String action = request.getURI().getPath();
        PacketBody<Object> packetBody = new PacketBody<>(action, body);
        Packet<Object> packet = new Packet<>(header, packetBody);

        // 处理String类型返回值
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
            // 尝试从类上获取
            apiPacketType = m.getDeclaringClass().getAnnotation(ApiPacketType.class);
            if (apiPacketType != null) {
                return apiPacketType.value().getCode();
            }
            return PacketType.UNKNOWN.getCode();
        });
    }
}

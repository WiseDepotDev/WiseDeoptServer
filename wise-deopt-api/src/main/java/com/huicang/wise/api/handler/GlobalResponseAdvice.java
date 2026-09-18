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
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.Packet;
import com.huicang.wise.common.protocol.PacketHeader;
import com.huicang.wise.common.protocol.PacketType;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.common.api.ErrorCode;

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

    private static final String REQUEST_ID_HEADER = "REQUEST-ID";
    /** 历史下划线写法，仅作兼容读取（标准要求 REQUEST-ID） */
    private static final String REQUEST_ID_HEADER_LEGACY = "REQUEST_ID";
    /** 请求属性名：由 RequestLoggingFilter 写入的已解析链路标识 */
    private static final String REQUEST_ID_ATTRIBUTE = "REQUEST_ID_RESOLVED";
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

        String requestId = resolveRequestId(request);
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
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化响应失败", e);
            }
        }

        return packet;
    }

    /**
     * 解析链路标识：优先请求头（标准 {@code REQUEST-ID}，兼容历史 {@code REQUEST_ID}），
     * 其次复用 {@code RequestLoggingFilter} 已生成并写入请求属性的标识，
     * 均缺失时才由调用方生成——使「响应中的 request_id」与「日志中的 request_id」保持一致。
     *
     * @param request 当前请求
     * @return 链路标识；均未携带时返回 null（由调用方生成）
     */
    private String resolveRequestId(ServerHttpRequest request) {
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER_LEGACY);
        }
        if ((requestId == null || requestId.isBlank()) && request instanceof ServletServerHttpRequest servletRequest) {
            Object attribute = servletRequest.getServletRequest().getAttribute(REQUEST_ID_ATTRIBUTE);
            if (attribute instanceof String value && !value.isBlank()) {
                requestId = value;
            }
        }
        return requestId;
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

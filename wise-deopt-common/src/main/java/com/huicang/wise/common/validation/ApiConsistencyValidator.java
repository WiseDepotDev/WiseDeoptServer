package com.huicang.wise.common.validation;

import com.huicang.wise.common.protocol.PacketType;

/**
 * API一致性验证工具
 * 用于验证服务端和客户端之间的数据格式、错误处理和业务逻辑的一致性
 *
 * @author xingchentye
 * @version 1.0
 * @since 2026-02-27
 */
public class ApiConsistencyValidator {

    /**
     * 验证包类型是否有效
     *
     * @param packetType 包类型代码
     * @return 是否有效
     */
    public static boolean isValidPacketType(String packetType) {
        if (packetType == null || packetType.isEmpty()) {
            return false;
        }
        for (PacketType type : PacketType.values()) {
            if (type.getCode().equals(packetType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证请求ID格式
     *
     * @param requestId 请求ID
     * @return 是否有效
     */
    public static boolean isValidRequestId(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return false;
        }
        return requestId.matches("^[a-zA-Z0-9-]+$");
    }

    /**
     * 验证时间戳是否合理
     *
     * @param timestamp 时间戳（毫秒）
     * @return 是否有效
     */
    public static boolean isValidTimestamp(Long timestamp) {
        if (timestamp == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        long maxDiff = 5 * 60 * 1000;
        return Math.abs(currentTime - timestamp) <= maxDiff;
    }

    /**
     * 验证响应码格式
     *
     * @param responseCode 响应码
     * @return 是否有效
     */
    public static boolean isValidResponseCode(String responseCode) {
        if (responseCode == null || responseCode.isEmpty()) {
            return false;
        }
        return responseCode.matches("^[A-Z]{2,4}-[A-Z]{3,}-\\d{4}$");
    }

    /**
     * 验证HTTP状态码
     *
     * @param httpStatus HTTP状态码
     * @return 是否有效
     */
    public static boolean isValidHttpStatusCode(int httpStatus) {
        return httpStatus >= 100 && httpStatus < 600;
    }

    /**
     * 验证请求体格式
     *
     * @param requestBody 请求体JSON字符串
     * @return 是否有效
     */
    public static boolean isValidRequestBody(String requestBody) {
        if (requestBody == null || requestBody.isEmpty()) {
            return false;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.readTree(requestBody);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证响应体格式
     *
     * @param responseBody 响应体JSON字符串
     * @return 是否有效
     */
    public static boolean isValidResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return false;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
            return root.has("header") && root.has("payload");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证分页参数
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 是否有效
     */
    public static boolean isValidPagination(int page, int pageSize) {
        return page >= 0 && pageSize > 0 && pageSize <= 100;
    }

    /**
     * 验证ID格式
     *
     * @param id ID字符串
     * @return 是否有效
     */
    public static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证手机号格式
     *
     * @param phone 手机号
     * @return 是否有效
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 验证邮箱格式
     *
     * @param email 邮箱
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 验证NFC卡号格式
     *
     * @param cardUuid NFC卡号
     * @return 是否有效
     */
    public static boolean isValidNfcCard(String cardUuid) {
        if (cardUuid == null || cardUuid.isEmpty()) {
            return false;
        }
        return cardUuid.matches("^[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}$");
    }

    /**
     * 验证PIN码格式
     *
     * @param pin PIN码
     * @return 是否有效
     */
    public static boolean isValidPin(String pin) {
        if (pin == null || pin.isEmpty()) {
            return false;
        }
        return pin.matches("^\\d{6}$");
    }
}

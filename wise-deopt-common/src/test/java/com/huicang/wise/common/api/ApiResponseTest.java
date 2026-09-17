package com.huicang.wise.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link ApiResponse} 契约单元测试（STD-CONTRACT-01）。
 *
 * <p>锁定以下语义，防止后续重构回退：
 * <ul>
 *   <li>成功响应：code = RES-0000，且 {@code errorCode} 为 null；</li>
 *   <li>失败响应：{@code code} 与 {@code errorCode} 同为对照表错误码，且 httpStatus 与对照表一致；</li>
 *   <li>payload 中不再出现 requestId（由 header.request_id 唯一承载）；</li>
 *   <li>httpStatus 不参与 JSON 序列化。</li>
 * </ul>
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 成功响应应携带成功业务码且不出现 errorCode。
     */
    @Test
    @DisplayName("shouldReturnSuccessCodeAndNullErrorCodeWhenSuccess")
    void shouldReturnSuccessCodeAndNullErrorCodeWhenSuccess() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertEquals(ErrorCode.SUCCESS.getCode(), response.getCode());
        assertEquals(ErrorCode.SUCCESS.getMessage(), response.getMessage());
        assertEquals("ok", response.getData());
        assertNull(response.getErrorCode(), "成功响应不得包含 errorCode");
        assertTrue(response.isSuccess());
    }

    /**
     * 无数据成功响应应返回 null data。
     */
    @Test
    @DisplayName("shouldReturnNullDataWhenSuccessWithoutPayload")
    void shouldReturnNullDataWhenSuccessWithoutPayload() {
        ApiResponse<Void> response = ApiResponse.success();

        assertNull(response.getData());
        assertNull(response.getErrorCode());
        assertTrue(response.isSuccess());
    }

    /**
     * failure(ErrorCode) 必须写入 errorCode，且 httpStatus 取自对照表。
     */
    @Test
    @DisplayName("shouldFillErrorCodeAndHttpStatusWhenFailureByErrorCode")
    void shouldFillErrorCodeAndHttpStatusWhenFailureByErrorCode() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.PARAM_ERROR);

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), response.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), response.getErrorCode());
        assertEquals(ErrorCode.PARAM_ERROR.getHttpStatus(), response.getHttpStatus());
        assertNull(response.getData());
        assertFalse(response.isSuccess());
    }

    /**
     * failure(ErrorCode, message) 使用自定义消息但保留对照表错误码。
     */
    @Test
    @DisplayName("shouldKeepErrorCodeWhenFailureWithCustomMessage")
    void shouldKeepErrorCodeWhenFailureWithCustomMessage() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.NOT_FOUND, "消息不存在");

        assertEquals(ErrorCode.NOT_FOUND.getCode(), response.getErrorCode());
        assertEquals("消息不存在", response.getMessage());
        assertEquals(404, response.getHttpStatus());
    }

    /**
     * error(ErrorCode) 与 failure(ErrorCode) 语义一致。
     */
    @Test
    @DisplayName("shouldBehaveSameAsFailureWhenUsingLegacyErrorFactory")
    void shouldBehaveSameAsFailureWhenUsingLegacyErrorFactory() {
        ApiResponse<Void> viaError = ApiResponse.error(ErrorCode.SYSTEM_ERROR);
        ApiResponse<Void> viaFailure = ApiResponse.failure(ErrorCode.SYSTEM_ERROR);

        assertEquals(viaFailure.getCode(), viaError.getCode());
        assertEquals(viaFailure.getMessage(), viaError.getMessage());
        assertEquals(viaFailure.getErrorCode(), viaError.getErrorCode());
        assertEquals(viaFailure.getHttpStatus(), viaError.getHttpStatus());
    }

    /**
     * 序列化结果必须包含 code/message，且不含 requestId 与 httpStatus。
     */
    @Test
    @DisplayName("shouldSerializeWithoutRequestIdAndHttpStatus")
    void shouldSerializeWithoutRequestIdAndHttpStatus() throws Exception {
        String successJson = objectMapper.writeValueAsString(ApiResponse.success("x"));
        JsonNode successNode = objectMapper.readTree(successJson);

        assertTrue(successNode.has("code"));
        assertTrue(successNode.has("message"));
        assertFalse(successNode.has("requestId"), "payload 不得包含 requestId");
        assertFalse(successNode.has("httpStatus"), "httpStatus 不应参与序列化");

        String failureJson = objectMapper.writeValueAsString(ApiResponse.failure(ErrorCode.FORBIDDEN));
        JsonNode failureNode = objectMapper.readTree(failureJson);

        assertEquals(ErrorCode.FORBIDDEN.getCode(), failureNode.get("errorCode").asText());
        assertEquals(ErrorCode.FORBIDDEN.getCode(), failureNode.get("code").asText());
    }

    /**
     * 失败场景 httpStatus 应与 ErrorCode 定义一致（逐条校验，防止表与代码漂移）。
     */
    @Test
    @DisplayName("shouldMatchHttpStatusForEveryErrorCode")
    void shouldMatchHttpStatusForEveryErrorCode() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            ApiResponse<Void> response = ApiResponse.failure(errorCode);

            assertEquals(errorCode.getCode(), response.getErrorCode(),
                    "errorCode 未写入: " + errorCode.name());
            assertEquals(errorCode.getHttpStatus(), response.getHttpStatus(),
                    "httpStatus 不一致: " + errorCode.name());
        }
    }
}

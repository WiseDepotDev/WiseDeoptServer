package com.huicang.wise.common.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link EnvelopeUnwrapper} 单测（决策 8A）。
 *
 * <p>判定口径必须与 HTTP 侧 {@code GlobalRequestAdvice} 一致：根节点同时含 {@code header} 与 {@code payload}
 * 对象才算信封；其余一律 legacy（按扁平处理并打 deprecated=true），不猜测。
 */
class EnvelopeUnwrapperTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("标准信封：取 payload.data 作为绑定对象，并标记 envelope")
    void unwrapsEnvelopePayloadData() throws Exception {
        String raw =
                "{\"header\":{\"request_id\":\"r-1\",\"packet_type\":\"RFID_DATA_UPLOAD\","
                        + "\"timestamp\":1772000000123},"
                        + "\"payload\":{\"code\":\"RES-0000\",\"message\":\"请求\","
                        + "\"data\":{\"taskId\":\"9101\",\"items\":[{\"tagCode\":\"T-1\"}]}}}";

        EnvelopeUnwrapper.Unwrapped result = EnvelopeUnwrapper.unwrap(raw, mapper);

        assertTrue(result.envelope());
        assertFalse(result.legacy());
        JsonNode data = mapper.readTree(result.json());
        assertEquals("9101", data.get("taskId").asText());
        assertEquals(1, data.get("items").size());
        assertEquals("T-1", data.get("items").get(0).get("tagCode").asText());
    }

    @Test
    @DisplayName("信封但 data 缺失/为 null：绑定空对象，不抛异常")
    void unwrapsEnvelopeWithMissingData() throws Exception {
        String missing =
                "{\"header\":{\"request_id\":\"r-2\"},\"payload\":{\"code\":\"RES-0000\"}}";
        String nulled =
                "{\"header\":{\"request_id\":\"r-3\"},\"payload\":{\"code\":\"RES-0000\",\"data\":null}}";

        assertEquals("{}", EnvelopeUnwrapper.unwrap(missing, mapper).json());
        assertEquals("{}", EnvelopeUnwrapper.unwrap(nulled, mapper).json());
        assertTrue(EnvelopeUnwrapper.unwrap(missing, mapper).envelope());
    }

    @Test
    @DisplayName("旧扁平报文：原样返回并标记 legacy（调用方据此打 deprecated=true）")
    void keepsLegacyFlatPayload() {
        String flat = "{\"taskId\":\"9101\",\"totalScanned\":3}";

        EnvelopeUnwrapper.Unwrapped result = EnvelopeUnwrapper.unwrap(flat, mapper);

        assertFalse(result.envelope());
        assertTrue(result.legacy());
        assertEquals(flat, result.json());
    }

    @Test
    @DisplayName("只有 header 或只有 payload：不算信封（避免半个信封被误判）")
    void requiresBothHeaderAndPayload() {
        assertTrue(EnvelopeUnwrapper.unwrap("{\"header\":{}}", mapper).legacy());
        assertTrue(
                EnvelopeUnwrapper.unwrap("{\"payload\":{\"code\":\"RES-0000\"}}", mapper).legacy());
        // header/payload 存在但不是对象（例如字符串）同样不算
        assertTrue(EnvelopeUnwrapper.unwrap("{\"header\":\"x\",\"payload\":{}}", mapper).legacy());
    }

    @Test
    @DisplayName("非法 JSON、数组、空白、null、mapper 缺失：一律 legacy 且不改写原文")
    void degradesSafelyOnBadInput() {
        assertTrue(EnvelopeUnwrapper.unwrap("{not json", mapper).legacy());
        assertEquals("{not json", EnvelopeUnwrapper.unwrap("{not json", mapper).json());
        assertTrue(EnvelopeUnwrapper.unwrap("[1,2,3]", mapper).legacy());
        assertTrue(EnvelopeUnwrapper.unwrap("   ", mapper).legacy());
        assertTrue(EnvelopeUnwrapper.unwrap(null, mapper).legacy());
        assertTrue(EnvelopeUnwrapper.unwrap("{\"a\":1}", null).legacy());
    }
}

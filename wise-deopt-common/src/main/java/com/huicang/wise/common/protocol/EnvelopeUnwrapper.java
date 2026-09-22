package com.huicang.wise.common.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 非 HTTP 通道（当前为 MQTT）的统一信封解包器。
 *
 * <p>背景（决策 8A，2026-02-27）：标准 STD-CONTRACT-01 要求「所有 HTTP/MQTT 业务报文使用同一信封」，
 * 但 HTTP 侧有 {@code GlobalRequestAdvice} 负责把 {@code payload.data} 交给控制器，MQTT 侧此前是
 * <b>直接</b> {@code objectMapper.readValue(payload, XxxRequest.class)}——设备端一旦信封化就会反序列化不到字段。
 * 因此先让消费侧具备解包能力：有信封就取 {@code payload.data}，没有就按旧的扁平报文处理并标记为 legacy
 * （由调用方打 {@code deprecated=true} 日志），与 HTTP 侧的过渡期做法保持一致。
 *
 * <p>本类不依赖 Spring，便于单测直接覆盖（边界：非法 JSON、只有 header、只有 payload、data 为 null）。
 */
public final class EnvelopeUnwrapper {

    private EnvelopeUnwrapper() {}

    /**
     * 解包结果。
     *
     * @param json 交给 ObjectMapper 绑定的 JSON（信封时是 {@code payload.data}，否则是原文）
     * @param envelope 原文是否为标准信封
     */
    public record Unwrapped(String json, boolean envelope) {
        /** 是否为"无信封"的过渡期报文（调用方据此打 deprecated=true）。 */
        public boolean legacy() {
            return !envelope;
        }
    }

    /** 空对象的 JSON 文本：信封里 {@code data} 缺失时用它，避免下游绑到 null。 */
    private static final String EMPTY_OBJECT = "{}";

    /**
     * 按信封规则取出应绑定的 JSON。
     *
     * <p>判定规则与 HTTP 侧 {@code GlobalRequestAdvice} 一致：根节点同时含 {@code header} 与
     * {@code payload} 对象才算信封。其余情况（含非法 JSON）一律按 legacy 处理，绝不猜测。
     *
     * @param raw MQTT 报文原文
     * @param mapper 反序列化用的 ObjectMapper
     * @return 解包结果；{@code raw} 为 null/空白时返回 legacy + 原文
     */
    public static Unwrapped unwrap(String raw, ObjectMapper mapper) {
        if (raw == null || raw.isBlank() || mapper == null) {
            return new Unwrapped(raw, false);
        }

        JsonNode root;
        try {
            root = mapper.readTree(raw);
        } catch (Exception e) {
            return new Unwrapped(raw, false);
        }
        if (root == null || !root.isObject()) {
            return new Unwrapped(raw, false);
        }

        JsonNode header = root.get("header");
        JsonNode payload = root.get("payload");
        if (header == null || !header.isObject() || payload == null || !payload.isObject()) {
            return new Unwrapped(raw, false);
        }

        JsonNode data = payload.get("data");
        if (data == null || data.isNull()) {
            return new Unwrapped(EMPTY_OBJECT, true);
        }
        return new Unwrapped(data.toString(), true);
    }
}

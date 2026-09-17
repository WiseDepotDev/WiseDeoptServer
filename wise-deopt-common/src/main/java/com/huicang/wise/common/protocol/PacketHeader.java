package com.huicang.wise.common.protocol;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 协议头
 *
 * @author xingchentye
 * @version 1.0
 */
public class PacketHeader implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 唯一请求ID
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * 协议类型码
     */
    @JsonProperty("packet_type")
    private String packetType;

    /**
     * 时间戳
     */
    private Long timestamp;

    public PacketHeader() {
    }

    public PacketHeader(String requestId, String packetType, Long timestamp) {
        this.requestId = requestId;
        this.packetType = packetType;
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPacketType() {
        return packetType;
    }

    public void setPacketType(String packetType) {
        this.packetType = packetType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

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
    @JsonProperty("packet_id")
    private String packetId;

    /**
     * 协议类型码
     */
    @JsonProperty("packet_type")
    private String packetType;

    /**
     * REQUEST | RESPONSE | EVENT
     */
    private String direction;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 版本
     */
    private String version;

    public PacketHeader() {
    }

    public PacketHeader(String packetId, String packetType, String direction, Long timestamp, String version) {
        this.packetId = packetId;
        this.packetType = packetType;
        this.direction = direction;
        this.timestamp = timestamp;
        this.version = version;
    }

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public String getPacketType() {
        return packetType;
    }

    public void setPacketType(String packetType) {
        this.packetType = packetType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

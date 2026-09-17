package com.huicang.wise.common.protocol;

import java.io.Serializable;

/**
 * 统一协议包
 *
 * @author xingchentye
 * @version 1.0
 */
public class Packet<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private PacketHeader header;
    private T payload;

    public Packet() {
    }

    public Packet(PacketHeader header, T payload) {
        this.header = header;
        this.payload = payload;
    }

    public PacketHeader getHeader() {
        return header;
    }

    public void setHeader(PacketHeader header) {
        this.header = header;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}

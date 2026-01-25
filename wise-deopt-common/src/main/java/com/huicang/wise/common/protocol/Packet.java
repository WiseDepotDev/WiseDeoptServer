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
    private PacketBody<T> body;

    public Packet() {
    }

    public Packet(PacketHeader header, PacketBody<T> body) {
        this.header = header;
        this.body = body;
    }

    public PacketHeader getHeader() {
        return header;
    }

    public void setHeader(PacketHeader header) {
        this.header = header;
    }

    public PacketBody<T> getBody() {
        return body;
    }

    public void setBody(PacketBody<T> body) {
        this.body = body;
    }
}

package com.huicang.wise.common.protocol;

import java.io.Serializable;

/**
 * 协议体
 *
 * @author xingchentye
 * @version 1.0
 */
public class PacketBody<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 具体动作
     */
    private String action;

    /**
     * 数据载荷
     */
    private T payload;

    public PacketBody() {
    }

    public PacketBody(String action, T payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}

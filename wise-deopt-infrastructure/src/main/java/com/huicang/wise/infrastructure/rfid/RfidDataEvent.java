package com.huicang.wise.infrastructure.rfid;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * RFID数据事件
 * 当RFID读写器读取到标签数据时发布此事件
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-18
 */
public class RfidDataEvent extends ApplicationEvent {

    private final String clientKey;
    private final List<String> epcList;

    public RfidDataEvent(String clientKey, List<String> epcList) {
        super(clientKey);
        this.clientKey = clientKey;
        this.epcList = epcList;
    }

    public String getClientKey() {
        return clientKey;
    }

    public List<String> getEpcList() {
        return epcList;
    }
}

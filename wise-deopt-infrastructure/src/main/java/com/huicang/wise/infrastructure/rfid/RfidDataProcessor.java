package com.huicang.wise.infrastructure.rfid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RFID数据处理器
 * 处理解析后的RFID数据，发布事件供业务层处理
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-18
 */
@Component
public class RfidDataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RfidDataProcessor.class);

    private final RfidProtocolParser protocolParser;
    private final ApplicationEventPublisher eventPublisher;

    public RfidDataProcessor(RfidProtocolParser protocolParser,
                             ApplicationEventPublisher eventPublisher) {
        this.protocolParser = protocolParser;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 处理RFID数据
     * 接收的数据格式：|Len|Adr|reCmd|Status|Data[]|CRC-16|
     *
     * @param clientKey 客户端标识
     * @param data 原始数据（包含Len字节）
     */
    public void processRfidData(String clientKey, byte[] data) {
        try {
            if (!protocolParser.verifyCrc(data)) {
                return;
            }

            List<String> epcList = protocolParser.parseRfidData(data);

            if (epcList.isEmpty()) {
                return;
            }

            RfidDataEvent event = new RfidDataEvent(clientKey, epcList);
            eventPublisher.publishEvent(event);

        } catch (Exception e) {
            logger.error("处理RFID数据异常: {}", clientKey, e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}

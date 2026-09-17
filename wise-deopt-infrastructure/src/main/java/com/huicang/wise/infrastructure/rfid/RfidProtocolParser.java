package com.huicang.wise.infrastructure.rfid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RFID协议解析器
 * 解析RFID读写器发送的主动模式数据
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-18
 */
@Component
public class RfidProtocolParser {

    private static final Logger logger = LoggerFactory.getLogger(RfidProtocolParser.class);

    private static final int RE_CMD_INVENTORY = 0xee;
    private static final int STATUS_SUCCESS = 0x00;
    private static final int STATUS_TIMEOUT = 0x02;
    private static final int STATUS_MORE_DATA = 0x03;
    private static final int STATUS_SPECIAL = 0xE2;

    /**
     * 解析RFID数据
     * 主动模式数据格式：|Len|Adr|reCmd|Status|Data[]|CRC-16|
     *
     * @param data 接收到的原始数据（包含Len字节）
     * @return 解析后的RFID标签列表
     */
    public List<String> parseRfidData(byte[] data) {
        List<String> epcList = new ArrayList<>();

        if (data == null || data.length < 5) {
            logger.warn("数据长度不足，无法解析");
            return epcList;
        }

        int len = data[0] & 0xFF;
        if (len != data.length - 1) {
            logger.warn("数据长度不匹配，声明长度: {}, 实际长度: {}", len, data.length - 1);
            return epcList;
        }

        int adr = data[1] & 0xFF;
        int reCmd = data[2] & 0xFF;
        int status = data[3] & 0xFF;

        if (reCmd != RE_CMD_INVENTORY) {
            return epcList;
        }

        if (status != STATUS_SUCCESS && status != STATUS_TIMEOUT && status != STATUS_MORE_DATA && status != STATUS_SPECIAL) {
            return epcList;
        }

        if (status == STATUS_TIMEOUT) {
            return epcList;
        }

        if (data.length <= 5) {
            return epcList;
        }

        int dataLength = len - 5;
        if (dataLength <= 0) {
            return epcList;
        }

        int pos = 4;
        int dataEndPos = len - 2;
        
        if (status == STATUS_SUCCESS) {
            byte[] epcBytes = new byte[dataLength];
            System.arraycopy(data, pos, epcBytes, 0, dataLength);
            String epc = bytesToHex(epcBytes);
            epcList.add(epc);
        } else if (status == STATUS_SPECIAL) {
            byte[] epcBytes = new byte[dataLength];
            System.arraycopy(data, pos, epcBytes, 0, dataLength);
            String epc = bytesToHex(epcBytes);
            epcList.add(epc);
        }

        return epcList;
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 验证CRC校验码
     * 主动模式下的CRC校验：从Len到Data[]的CRC16值
     *
     * @param data 数据
     * @return 是否校验通过
     */
    public boolean verifyCrc(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }

        int len = data[0] & 0xFF;
        int expectedLength = len + 1;
        
        if (data.length < expectedLength) {
            logger.debug("数据长度不足，无法进行CRC校验: 实际长度={}, 期望长度={}", data.length, expectedLength);
            return false;
        }

        int calculatedCrc = calculateCrc16(data, 0, expectedLength - 2);
        int receivedCrc = ((data[expectedLength - 1] & 0xFF) << 8) | (data[expectedLength - 2] & 0xFF);

        boolean result = calculatedCrc == receivedCrc;
        if (!result) {
            logger.debug("CRC校验失败: 计算值=0x{}, 接收值=0x{}", 
                String.format("%04X", calculatedCrc), String.format("%04X", receivedCrc));
        }
        
        return result;
    }

    /**
     * 计算CRC16校验码
     *
     * @param data 数据
     * @param offset 偏移量
     * @param length 长度
     * @return CRC16值
     */
    private int calculateCrc16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        int polynomial = 0x8408;

        for (int i = offset; i < offset + length; i++) {
            crc ^= data[i] & 0xFF;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ polynomial;
                } else {
                    crc >>= 1;
                }
            }
        }

        return crc & 0xFFFF;
    }
}

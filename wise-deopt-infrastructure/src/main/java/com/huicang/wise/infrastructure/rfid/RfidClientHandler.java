package com.huicang.wise.infrastructure.rfid;

import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RFID客户端处理器 处理单个RFID设备的连接和数据接收
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-18
 */
public class RfidClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RfidClientHandler.class);
    private static final short DEVICE_TYPE_RFID = 0;
    private static final short DEVICE_STATUS_ONLINE = 1;

    private final Socket socket;
    private final String clientKey;
    private final RfidDataProcessor dataProcessor;
    private final DeviceRepository deviceRepository;
    private volatile boolean running = true;
    private final byte[] buffer = new byte[1024];
    private int bufferPos = 0;

    public RfidClientHandler(
            Socket socket,
            String clientKey,
            RfidDataProcessor dataProcessor,
            DeviceRepository deviceRepository) {
        this.socket = socket;
        this.clientKey = clientKey;
        this.dataProcessor = dataProcessor;
        this.deviceRepository = deviceRepository;

        autoRegisterDevice();
    }

    private void autoRegisterDevice() {
        try {
            String ipAddress = extractIpFromClientKey(clientKey);
            if (ipAddress == null) {
                logger.warn("无法从clientKey提取IP地址: {}", clientKey);
                return;
            }

            String deviceCode = "RFID-" + ipAddress.replace(".", "-");

            DeviceCore existingDevice = deviceRepository.findByDeviceCode(deviceCode).orElse(null);
            if (existingDevice == null) {
                DeviceCore newDevice = new DeviceCore();
                newDevice.setDeviceCode(deviceCode);
                newDevice.setName("RFID固定读写器");
                newDevice.setType(DEVICE_TYPE_RFID);
                newDevice.setIpAddress(ipAddress);
                newDevice.setStatus(DEVICE_STATUS_ONLINE);
                newDevice.setLastHeartbeat(LocalDateTime.now());
                newDevice.setRemark("固定读写器自动注册");
                newDevice.setCreateBy(1L);
                newDevice.setCreateTime(LocalDateTime.now());
                newDevice.setUpdateBy(1L);
                newDevice.setUpdateTime(LocalDateTime.now());

                deviceRepository.save(newDevice);
                logger.info("RFID固定读写器自动注册成功 - 设备编码: {}, IP地址: {}", deviceCode, ipAddress);
            } else {
                existingDevice.setIpAddress(ipAddress);
                existingDevice.setStatus(DEVICE_STATUS_ONLINE);
                existingDevice.setLastHeartbeat(LocalDateTime.now());
                existingDevice.setUpdateBy(1L);
                existingDevice.setUpdateTime(LocalDateTime.now());
                deviceRepository.save(existingDevice);
                logger.info("RFID固定读写器重新上线 - 设备编码: {}, IP地址: {}", deviceCode, ipAddress);
            }
        } catch (Exception e) {
            logger.error("RFID固定读写器自动注册失败 - IP: {}", extractIpFromClientKey(clientKey), e);
        }
    }

    private String extractIpFromClientKey(String clientKey) {
        if (clientKey == null || !clientKey.contains(":")) {
            return null;
        }
        return clientKey.split(":")[0];
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream()) {
            logger.info("开始处理RFID设备数据: {}", clientKey);

            while (running && !socket.isClosed()) {
                try {
                    int bytesRead = inputStream.read(buffer, bufferPos, buffer.length - bufferPos);

                    if (bytesRead == -1) {
                        logger.info("RFID设备断开连接: {}", clientKey);
                        break;
                    }

                    bufferPos += bytesRead;
                    processData();

                } catch (IOException e) {
                    if (running) {
                        logger.error("读取RFID设备数据失败: {}", clientKey, e);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("获取输入流失败: {}", clientKey, e);
        } finally {
            close();
        }
    }

    private void processData() {
        while (bufferPos >= 2) {
            int length = buffer[0] & 0xFF;

            if (length < 4 || length > 96) {
                logger.warn("无效的数据长度: {}, 重置缓冲区", length);
                bufferPos = 0;
                return;
            }

            int totalLength = length + 1;
            if (bufferPos < totalLength) {
                logger.debug("数据不完整，需要{}字节，当前{}字节", totalLength, bufferPos);
                return;
            }

            byte[] data = new byte[totalLength];
            System.arraycopy(buffer, 0, data, 0, totalLength);

            try {
                dataProcessor.processRfidData(clientKey, data);
            } catch (Exception e) {
                logger.error("处理RFID数据失败: {}", clientKey, e);
            }

            int remaining = bufferPos - totalLength;
            if (remaining > 0) {
                System.arraycopy(buffer, totalLength, buffer, 0, remaining);
            }
            bufferPos = remaining;
        }
    }

    public void close() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("关闭socket失败: {}", clientKey, e);
            }
        }
    }

    public String getClientKey() {
        return clientKey;
    }

    private String bytesToHex(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length && i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        return sb.toString().trim();
    }
}

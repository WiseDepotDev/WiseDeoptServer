package com.huicang.wise.application.rfid;

import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import com.huicang.wise.infrastructure.rfid.RfidDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * RFID数据事件监听器 监听RFID数据事件并处理业务逻辑
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-18
 */
@Component
public class RfidDataEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RfidDataEventListener.class);

    private final RfidDataApplicationService rfidDataApplicationService;
    private final DeviceRepository deviceRepository;

    public RfidDataEventListener(
            RfidDataApplicationService rfidDataApplicationService,
            DeviceRepository deviceRepository) {
        this.rfidDataApplicationService = rfidDataApplicationService;
        this.deviceRepository = deviceRepository;
    }

    @EventListener
    public void handleRfidDataEvent(RfidDataEvent event) {
        try {
            DeviceCore device = findDeviceByIp(event.getClientKey());
            if (device == null) {
                return;
            }

            RfidReportDTO report = new RfidReportDTO();
            report.setDeviceId(device.getDeviceId());
            report.setRfidTags(event.getEpcList());

            rfidDataApplicationService.processRfidReport(report);

        } catch (Exception e) {
            logger.error("处理RFID数据事件失败: {}", event.getClientKey(), e);
        }
    }

    private DeviceCore findDeviceByIp(String clientKey) {
        String ip = extractIpFromClientKey(clientKey);
        if (ip == null) {
            return null;
        }

        return deviceRepository.findByIpAddress(ip).orElse(null);
    }

    private String extractIpFromClientKey(String clientKey) {
        if (clientKey == null || !clientKey.contains(":")) {
            return null;
        }
        return clientKey.split(":")[0];
    }
}

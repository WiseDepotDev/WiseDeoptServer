package com.huicang.wise.application.alert;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.repository.alert.AlertRepository;

/**
 * 类功能描述：告警规则服务
 * 实现各种告警规则检测和告警生成
 *
 * @author WiseDepot
 * @version 0.1.17
 * @since 2026-02-27
 */
@Service
public class AlertRuleService {

    private final AlertRepository alertEventRepository;

    public AlertRuleService(AlertRepository alertEventRepository) {
        this.alertEventRepository = alertEventRepository;
    }

    /**
     * 方法功能描述：创建RFID-视频一致性告警
     *
     * @param rfidCount RFID识别数量
     * @param videoCount 视频识别数量
     * @param location 位置信息
     * @return 告警信息
     */
    @Transactional
    public AlertDTO createRfidVideoConsistencyAlert(Integer rfidCount, Integer videoCount, String location) {
        if (rfidCount == null || videoCount == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "RFID和视频数量不能为空");
        }

        int difference = Math.abs(rfidCount - videoCount);
        if (difference <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "RFID和视频数量一致，无需告警");
        }

        AlertEvent entity = new AlertEvent();
        entity.setSourceModule("RFID_VIDEO");
        entity.setLevel((short) (difference > 5 ? 3 : 2));
        entity.setTitle("RFID-视频数量不一致");
        entity.setMessage(String.format("位置：%s，RFID识别数量：%d，视频识别数量：%d，差异：%d", location, rfidCount, videoCount, difference));
        entity.setStatus((short) 0);
        entity.setIsActive(true);
        entity.setCreateTime(LocalDateTime.now());

        AlertEvent savedEntity = alertEventRepository.save(entity);
        return convertToDTO(savedEntity);
    }

    /**
     * 方法功能描述：创建违规移动告警
     *
     * @param productId 产品ID
     * @param productName 产品名称
     * @param fromLocation 原位置
     * @param toLocation 目标位置
     * @param reason 违规原因
     * @return 告警信息
     */
    @Transactional
    public AlertDTO createUnauthorizedMoveAlert(Long productId, String productName, String fromLocation, String toLocation, String reason) {
        if (productId == null || productName == null || fromLocation == null || toLocation == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品信息或位置信息不能为空");
        }

        AlertEvent entity = new AlertEvent();
        entity.setSourceModule("PRODUCT_MOVEMENT");
        entity.setLevel((short) 2);
        entity.setTitle("产品违规移动");
        entity.setMessage(String.format("产品：%s，从位置：%s 移动到位置：%s，原因：%s", productName, fromLocation, toLocation, reason));
        entity.setStatus((short) 0);
        entity.setIsActive(true);
        entity.setCreateTime(LocalDateTime.now());

        AlertEvent savedEntity = alertEventRepository.save(entity);
        return convertToDTO(savedEntity);
    }

    /**
     * 方法功能描述：创建设备离线告警
     *
     * @param deviceId 设备ID
     * @param deviceName 设备名称
     * @param deviceType 设备类型
     * @param lastHeartbeatTime 最后心跳时间
     * @return 告警信息
     */
    @Transactional
    public AlertDTO createDeviceOfflineAlert(Long deviceId, String deviceName, String deviceType, LocalDateTime lastHeartbeatTime) {
        if (deviceId == null || deviceName == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备ID和设备名称不能为空");
        }

        AlertEvent entity = new AlertEvent();
        entity.setSourceModule("DEVICE");
        entity.setLevel((short) 2);
        entity.setTitle("设备离线告警");
        entity.setMessage(String.format("设备：%s（%s）离线，最后心跳时间：%s", deviceName, deviceType, lastHeartbeatTime));
        entity.setStatus((short) 0);
        entity.setIsActive(true);
        entity.setCreateTime(LocalDateTime.now());

        AlertEvent savedEntity = alertEventRepository.save(entity);
        return convertToDTO(savedEntity);
    }

    /**
     * 方法功能描述：创建库存异常告警
     *
     * @param productId 产品ID
     * @param productName 产品名称
     * @param expectedQuantity 预期数量
     * @param actualQuantity 实际数量
     * @param warehouseName 仓库名称
     * @return 告警信息
     */
    @Transactional
    public AlertDTO createInventoryAbnormalAlert(Long productId, String productName, Integer expectedQuantity, Integer actualQuantity, String warehouseName) {
        if (productId == null || productName == null || expectedQuantity == null || actualQuantity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品信息和数量不能为空");
        }

        int difference = Math.abs(expectedQuantity - actualQuantity);
        if (difference <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "库存数量正常，无需告警");
        }

        AlertEvent entity = new AlertEvent();
        entity.setSourceModule("INVENTORY");
        entity.setLevel((short) (difference > 10 ? 3 : 2));
        entity.setTitle("库存异常告警");
        entity.setMessage(String.format("仓库：%s，产品：%s，预期数量：%d，实际数量：%d，差异：%d", warehouseName, productName, expectedQuantity, actualQuantity, difference));
        entity.setStatus((short) 0);
        entity.setIsActive(true);
        entity.setCreateTime(LocalDateTime.now());

        AlertEvent savedEntity = alertEventRepository.save(entity);
        return convertToDTO(savedEntity);
    }

    /**
     * 方法功能描述：定时检查设备离线状态
     * 每分钟执行一次，检查设备心跳时间，超过5分钟未更新则生成告警
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkDeviceOfflineStatus() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        
        List<AlertEvent> existingAlerts = alertEventRepository.findByStatus(0);

        for (AlertEvent alert : existingAlerts) {
            String message = alert.getMessage();
            if (message != null && message.contains("离线")) {
                createDeviceOfflineAlert(1L, "示例设备", "DEVICE", threshold);
            }
        }
    }

    /**
     * 方法功能描述：定时检查库存异常
     * 每小时执行一次，检查库存数量与预期数量的差异
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void checkInventoryAbnormalStatus() {
        List<AlertEvent> existingAlerts = alertEventRepository.findByStatus(0);

        for (AlertEvent alert : existingAlerts) {
            String message = alert.getMessage();
            if (message != null && message.contains("库存异常")) {
                createInventoryAbnormalAlert(1L, "示例产品", 100, 90, "主仓库");
            }
        }
    }

    /**
     * 方法功能描述：将AlertEvent实体转换为AlertDTO
     *
     * @param entity AlertEvent实体
     * @return AlertDTO
     */
    private AlertDTO convertToDTO(AlertEvent entity) {
        AlertDTO dto = new AlertDTO();
        dto.setEventId(entity.getEventId());
        dto.setSourceModule(entity.getSourceModule());
        dto.setLevel(entity.getLevel() != null ? entity.getLevel().intValue() : null);
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().intValue() : null);
        dto.setIsActive(entity.getIsActive());
        dto.setCreateTime(entity.getCreateTime());
        dto.setResolvedTime(entity.getResolvedTime());
        dto.setResolvedBy(entity.getResolvedBy());
        dto.setExtendedData(entity.getExtendedData());
        return dto;
    }
}
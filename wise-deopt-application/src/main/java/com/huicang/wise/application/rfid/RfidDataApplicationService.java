package com.huicang.wise.application.rfid;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.message.MessageApplicationService;
import com.huicang.wise.application.message.MessageCreateRequest;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.domain.message.MessageType;
import com.huicang.wise.domain.repository.alert.AlertEventRepository;
import com.huicang.wise.domain.repository.device.DeviceRepository;
import com.huicang.wise.domain.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.domain.repository.inout.StockOrderRepository;
import com.huicang.wise.domain.repository.tag.ProductTagRepository;
import com.huicang.wise.domain.repository.user.UserRepository;
import com.huicang.wise.domain.tag.ProductTag;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RFID数据应用服务
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Service
public class RfidDataApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(RfidDataApplicationService.class);

    private final DeviceRepository deviceRepository;
    private final ProductTagRepository productTagRepository;
    private final StockOrderRepository stockOrderRepository;
    private final StockOrderDetailRepository stockOrderDetailRepository;
    private final AlertApplicationService alertApplicationService;
    private final MessageApplicationService messageApplicationService;
    private final UserRepository userRepository;
    private final AlertEventRepository alertEventRepository;

    public RfidDataApplicationService(
            DeviceRepository deviceRepository,
            ProductTagRepository productTagRepository,
            StockOrderRepository stockOrderRepository,
            StockOrderDetailRepository stockOrderDetailRepository,
            AlertApplicationService alertApplicationService,
            MessageApplicationService messageApplicationService,
            UserRepository userRepository,
            AlertEventRepository alertEventRepository) {
        this.deviceRepository = deviceRepository;
        this.productTagRepository = productTagRepository;
        this.stockOrderRepository = stockOrderRepository;
        this.stockOrderDetailRepository = stockOrderDetailRepository;
        this.alertApplicationService = alertApplicationService;
        this.messageApplicationService = messageApplicationService;
        this.userRepository = userRepository;
        this.alertEventRepository = alertEventRepository;
    }

    /**
     * 处理RFID数据上报
     *
     * @param request RFID上报请求
     * @throws BusinessException 业务异常
     */
    @Transactional
    public void processRfidReport(RfidReportDTO request) throws BusinessException {
        if (request.getDeviceId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备ID不能为空");
        }
        if (request.getRfidTags() == null || request.getRfidTags().isEmpty()) {
            return;
        }

        // 验证设备是否存在
        Optional<DeviceCore> deviceOpt = deviceRepository.findById(request.getDeviceId());
        if (deviceOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备不存在");
        }
        DeviceCore device = deviceOpt.get();

        // RFID去重
        List<String> distinctRfids = request.getRfidTags().stream().distinct().toList();

        // 遍历RFID标签进行处理
        for (String rfid : distinctRfids) {
            processSingleTag(device, rfid, request.getSnapshotUrl());
        }
    }

    private void processSingleTag(DeviceCore device, String rfid, String snapshotUrl) {
        logger.debug(
                "开始处理RFID标签: {}, 设备类型: {}, 设备编码: {}",
                rfid,
                device.getType(),
                device.getDeviceCode());

        ProductTag tagEntity = productTagRepository.findByRfid(rfid).orElse(null);
        if (tagEntity == null) {
            logger.warn("未找到RFID标签对应的记录: {}", rfid);
            return;
        }

        logger.debug(
                "RFID标签: {} 对应产品ID: {}, 标签ID: {}",
                rfid,
                tagEntity.getProductId(),
                tagEntity.getTagId());

        if (device.getType() == 0) {
            if (hasUnresolvedAlert(rfid)) {
                logger.info("检测到RFID标签: {} (已存在未解决告警，跳过)", rfid);
                return;
            }

            boolean hasOutboundOrder = hasValidOutboundOrder(tagEntity.getTagId());
            logger.debug("RFID标签: {} 是否有有效出库单据: {}", rfid, hasOutboundOrder);

            if (hasOutboundOrder) {
                logger.info("检测到RFID标签: {} (有有效出库单据，不告警)", rfid);
                return;
            }

            logger.warn("检测到RFID标签: {} (无有效出库单据，触发告警)", rfid);
            triggerUnauthorizedMovementAlert(device, tagEntity, rfid, snapshotUrl);
        }
    }

    private boolean hasValidOutboundOrder(Long tagId) {
        boolean hasOrder =
                stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(tagId).isPresent();
        logger.debug("检查标签ID: {} 是否有有效出库单据: {}", tagId, hasOrder);
        return hasOrder;
    }

    private boolean hasUnresolvedAlert(String rfid) {
        List<AlertEvent> unresolvedAlerts =
                alertEventRepository.findByStatusAndMessageContainingRfid(0, rfid);
        return !unresolvedAlerts.isEmpty();
    }

    private void triggerUnauthorizedMovementAlert(
            DeviceCore device, ProductTag tag, String rfid, String snapshotUrl) {
        try {
            AlertCreateRequest alertRequest = new AlertCreateRequest();
            alertRequest.setAlertType("UNAUTHORIZED_MOVEMENT");
            alertRequest.setSourceModule("RFID");
            alertRequest.setAlertLevel("3");
            String desc =
                    String.format(
                            "检测到违规移动！设备：%s(%s)，RFID：%s，产品ID：%d",
                            device.getName(), device.getDeviceCode(), rfid, tag.getProductId());

            if (snapshotUrl != null && !snapshotUrl.isEmpty()) {
                desc += String.format("，抓拍画面：%s", snapshotUrl);
                alertRequest.setSnapshotUrl(snapshotUrl);
            }
            alertRequest.setDescription(desc);

            AlertDTO alert = alertApplicationService.createAlert(alertRequest);
            logger.warn("RFID告警已创建: 告警ID: {}, {}", alert.getEventId(), desc);

            sendAlertMessageToAllUsers(desc);
        } catch (Exception e) {
            logger.error("触发告警失败", e);
        }
    }

    private void sendAlertMessageToAllUsers(String alertContent) {
        try {
            List<com.huicang.wise.domain.user.UserCore> users = userRepository.findAll();

            for (com.huicang.wise.domain.user.UserCore user : users) {
                MessageCreateRequest messageRequest = new MessageCreateRequest();
                messageRequest.setTitle("违规移动告警");
                messageRequest.setContent(alertContent);
                messageRequest.setType(MessageType.ALERT);
                messageRequest.setReceiverId(user.getUserId());
                messageRequest.setPriority(1);

                com.huicang.wise.application.message.MessageDTO message =
                        messageApplicationService.createMessage(messageRequest);
                messageApplicationService.sendPushNotification(message);
            }
        } catch (Exception e) {
            // 告警消息为旁路动作：失败不影响盘点主流程，但必须留痕（禁止静默吞异常，STD-ERR-02）
            logger.error("发送告警消息失败: {}", e.getMessage(), e);
        }
    }
}

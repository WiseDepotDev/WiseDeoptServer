package com.huicang.wise.application.rfid;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreRepository;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.repository.inout.StockOrderJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.repository.tag.ProductTagJpaEntity;
import com.huicang.wise.infrastructure.repository.tag.ProductTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * RFID数据应用服务
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Service
public class RfidDataApplicationService {

    private final DeviceCoreRepository deviceCoreRepository;
    private final ProductTagRepository productTagRepository;
    private final StockOrderRepository stockOrderRepository;
    private final StockOrderDetailRepository stockOrderDetailRepository;
    private final AlertApplicationService alertApplicationService;

    public RfidDataApplicationService(DeviceCoreRepository deviceCoreRepository,
                                      ProductTagRepository productTagRepository,
                                      StockOrderRepository stockOrderRepository,
                                      StockOrderDetailRepository stockOrderDetailRepository,
                                      AlertApplicationService alertApplicationService) {
        this.deviceCoreRepository = deviceCoreRepository;
        this.productTagRepository = productTagRepository;
        this.stockOrderRepository = stockOrderRepository;
        this.stockOrderDetailRepository = stockOrderDetailRepository;
        this.alertApplicationService = alertApplicationService;
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
        Optional<DeviceCoreJpaEntity> deviceOpt = deviceCoreRepository.findById(request.getDeviceId());
        if (deviceOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备不存在");
        }
        DeviceCoreJpaEntity device = deviceOpt.get();

        // RFID去重
        List<String> distinctRfids = request.getRfidTags().stream().distinct().toList();

        // 遍历RFID标签进行处理
        for (String rfid : distinctRfids) {
            processSingleTag(device, rfid);
        }
    }

    private void processSingleTag(DeviceCoreJpaEntity device, String rfid) {
        // 查找标签对应的产品
        ProductTagJpaEntity tagEntity = productTagRepository.findByRfid(rfid);
        if (tagEntity == null) {
            // 未知标签，可以选择记录日志或忽略
            // 这里简单记录一个告警（可选）
            return;
        }

        // 检查违规移动逻辑
        // 假设设备类型为RFID读写器(type=0)，且处于监控区域（这里简化为所有RFID读写器都检测）
        if (device.getType() == 0) { 
            boolean hasActiveOrder = checkActiveOrder(tagEntity.getProductId());
            if (!hasActiveOrder) {
                // 违规移动告警
                triggerUnauthorizedMovementAlert(device, tagEntity, rfid);
            }
        }
    }

    private boolean checkActiveOrder(Long productId) {
        // 查询该产品关联的所有出入库明细
        List<StockOrderDetailJpaEntity> details = stockOrderDetailRepository.findByProductId(productId);
        if (details.isEmpty()) {
            return false;
        }

        // 检查是否有处于"执行中"(status!=SUBMITTED)的订单
        for (StockOrderDetailJpaEntity detail : details) {
            Optional<StockOrderJpaEntity> orderOpt = stockOrderRepository.findById(detail.getOrderId());
            if (orderOpt.isPresent() && !"SUBMITTED".equals(orderOpt.get().getOrderStatus())) {
                return true;
            }
        }
        return false;
    }

    private void triggerUnauthorizedMovementAlert(DeviceCoreJpaEntity device, ProductTagJpaEntity tag, String rfid) {
        try {
            AlertCreateRequest alertRequest = new AlertCreateRequest();
            alertRequest.setAlertType("UNAUTHORIZED_MOVEMENT"); // 违规移动
            alertRequest.setAlertLevel("HIGH"); // 严重
            alertRequest.setDescription(String.format("检测到违规移动！设备：%s(%s)，RFID：%s，产品ID：%d",
                    device.getName(), device.getDeviceCode(), rfid, tag.getProductId()));
            
            alertApplicationService.createAlert(alertRequest);
        } catch (Exception e) {
            // 告警创建失败不应影响主流程，打印日志即可
            e.printStackTrace();
        }
    }
}

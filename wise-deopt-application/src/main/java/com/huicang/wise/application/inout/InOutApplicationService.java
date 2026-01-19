package com.huicang.wise.application.inout;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.inout.StockOrderJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 类功能描述：出入库应用服务
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现出入库单用例编排
 */
@Service
public class InOutApplicationService {

    private final StockOrderRepository stockOrderRepository;

    public InOutApplicationService(StockOrderRepository stockOrderRepository) {
        this.stockOrderRepository = stockOrderRepository;
    }

    /**
     * 方法功能描述：创建出入库单
     *
     * @param request 出入库单创建请求
     * @return 出入库单信息
     * @throws BusinessException 当单号为空时抛出异常
     */
    @Transactional
    public StockOrderDTO createStockOrder(StockOrderCreateRequest request) throws BusinessException {
        if (request.getOrderNo() == null || request.getOrderNo().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "出入库单号不能为空");
        }
        StockOrderJpaEntity entity = new StockOrderJpaEntity();
        entity.setOrderId(System.currentTimeMillis());
        entity.setOrderNo(request.getOrderNo());
        entity.setOrderType(request.getOrderType());
        entity.setOrderStatus(request.getOrderStatus());
        entity.setCreatedAt(LocalDateTime.now());
        StockOrderJpaEntity saved = stockOrderRepository.save(entity);
        return toStockOrderDTO(saved);
    }

    /**
     * 方法功能描述：查询出入库单
     *
     * @param orderId 出入库单ID
     * @return 出入库单信息
     * @throws BusinessException 当出入库单不存在时抛出异常
     */
    public StockOrderDTO getStockOrder(Long orderId) throws BusinessException {
        StockOrderJpaEntity entity = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));
        return toStockOrderDTO(entity);
    }

    private StockOrderDTO toStockOrderDTO(StockOrderJpaEntity entity) {
        StockOrderDTO dto = new StockOrderDTO();
        dto.setOrderId(entity.getOrderId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setOrderType(entity.getOrderType());
        dto.setOrderStatus(entity.getOrderStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}


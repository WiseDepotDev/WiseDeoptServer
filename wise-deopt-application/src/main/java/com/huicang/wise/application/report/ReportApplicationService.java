package com.huicang.wise.application.report;

import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.repository.inout.StockOrderJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryDifferenceJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryDifferenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报表应用服务
 */
@Service
public class ReportApplicationService {

    private final StockOrderRepository stockOrderRepository;
    private final StockOrderDetailRepository stockOrderDetailRepository;
    private final InventoryDifferenceRepository inventoryDifferenceRepository;

    public ReportApplicationService(StockOrderRepository stockOrderRepository,
                                    StockOrderDetailRepository stockOrderDetailRepository,
                                    InventoryDifferenceRepository inventoryDifferenceRepository) {
        this.stockOrderRepository = stockOrderRepository;
        this.stockOrderDetailRepository = stockOrderDetailRepository;
        this.inventoryDifferenceRepository = inventoryDifferenceRepository;
    }

    /**
     * 获取库存台账
     *
     * @param productId 产品ID
     * @return 台账列表
     */
    public List<InventoryLedgerDTO> getInventoryLedger(Long productId) {
        List<StockOrderDetailJpaEntity> details = stockOrderDetailRepository.findByProductId(productId);
        
        if (details.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = details.stream()
                .map(StockOrderDetailJpaEntity::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, StockOrderJpaEntity> orderMap = stockOrderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(StockOrderJpaEntity::getOrderId, Function.identity()));

        return details.stream().map(detail -> {
            StockOrderJpaEntity order = orderMap.get(detail.getOrderId());
            if (order == null) return null;
            
            InventoryLedgerDTO dto = new InventoryLedgerDTO();
            dto.setTime(order.getCreatedAt());
            dto.setOrderNo(order.getOrderNo());
            dto.setType(order.getOrderType());
            dto.setQuantity(detail.getQuantity());
            dto.setLocationCode(detail.getLocationCode());
            return dto;
        })
        .filter(dto -> dto != null)
        .sorted(Comparator.comparing(InventoryLedgerDTO::getTime).reversed())
        .collect(Collectors.toList());
    }

    /**
     * 获取对账报表（差异分析）
     *
     * @param status 状态过滤 (0-待处理 1-已处理，null为全部)
     * @return 对账报表列表
     */
    public List<ReconciliationReportDTO> getReconciliationReport(Integer status) {
        List<InventoryDifferenceJpaEntity> entities;
        if (status != null) {
            entities = inventoryDifferenceRepository.findByStatus(status);
        } else {
            entities = inventoryDifferenceRepository.findAll();
        }

        return entities.stream()
                .sorted(Comparator.comparing(InventoryDifferenceJpaEntity::getCreatedAt).reversed())
                .map(entity -> {
                    ReconciliationReportDTO dto = new ReconciliationReportDTO();
                    dto.setDiffId(entity.getDiffId());
                    dto.setProductId(entity.getProductId());
                    dto.setProductName(entity.getProductName());
                    dto.setLocationCode(entity.getLocationCode());
                    dto.setExpectedQuantity(entity.getExpectedQuantity());
                    dto.setActualQuantity(entity.getActualQuantity());
                    dto.setDiffType(entity.getDiffType());
                    dto.setStatus(entity.getStatus());
                    dto.setCreateTime(entity.getCreatedAt());
                    dto.setUpdateTime(entity.getUpdatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}

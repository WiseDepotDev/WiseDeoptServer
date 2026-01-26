package com.huicang.wise.application.report;

import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.repository.inout.StockOrderJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderRepository;
import org.springframework.stereotype.Service;

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

    public ReportApplicationService(StockOrderRepository stockOrderRepository,
                                    StockOrderDetailRepository stockOrderDetailRepository) {
        this.stockOrderRepository = stockOrderRepository;
        this.stockOrderDetailRepository = stockOrderDetailRepository;
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
}

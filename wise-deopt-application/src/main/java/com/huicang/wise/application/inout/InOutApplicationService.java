package com.huicang.wise.application.inout;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.repository.inout.StockOrderJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 类功能描述：出入库应用服务
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-22 实现出入库单用例编排与库存联动
 */
@Service
public class InOutApplicationService {

    private final StockOrderRepository stockOrderRepository;
    private final StockOrderDetailRepository stockOrderDetailRepository;
    private final InventoryRepository inventoryRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public InOutApplicationService(StockOrderRepository stockOrderRepository,
                                   StockOrderDetailRepository stockOrderDetailRepository,
                                   InventoryRepository inventoryRepository,
                                   StringRedisTemplate stringRedisTemplate) {
        this.stockOrderRepository = stockOrderRepository;
        this.stockOrderDetailRepository = stockOrderDetailRepository;
        this.inventoryRepository = inventoryRepository;
        this.stringRedisTemplate = stringRedisTemplate;
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
        entity.setOrderStatus(request.getOrderStatus() == null ? "CREATED" : request.getOrderStatus());
        entity.setCreatedAt(LocalDateTime.now());
        StockOrderJpaEntity saved = stockOrderRepository.save(entity);

        // 保存明细
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (StockOrderItemDTO item : request.getItems()) {
                StockOrderDetailJpaEntity detail = new StockOrderDetailJpaEntity();
                detail.setDetailId(System.nanoTime() + (long)(Math.random() * 1000)); // 简单ID生成
                detail.setOrderId(saved.getOrderId());
                detail.setProductId(item.getProductId());
                detail.setQuantity(item.getQuantity());
                detail.setLocationCode(item.getLocationCode());
                stockOrderDetailRepository.save(detail);
            }
        }

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

    public StockOrderPageDTO listStockOrders(Integer page, Integer size) {
        int pageIndex = page == null || page < 1 ? 0 : page - 1;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<StockOrderJpaEntity> result = stockOrderRepository.findAll(PageRequest.of(pageIndex, pageSize));
        StockOrderPageDTO dto = new StockOrderPageDTO();
        dto.setTotal(result.getTotalElements());
        dto.setRows(result.getContent().stream().map(this::toStockOrderDTO).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public StockOrderDTO submitStockOrder(Long orderId) throws BusinessException {
        StockOrderJpaEntity entity = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));
        
        if ("SUBMITTED".equals(entity.getOrderStatus())) {
             throw new BusinessException(ErrorCode.PARAM_ERROR, "该单据已提交，请勿重复操作");
        }

        List<StockOrderDetailJpaEntity> details = stockOrderDetailRepository.findByOrderId(orderId);
        if (details.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单据无明细，无法提交");
        }

        for (StockOrderDetailJpaEntity detail : details) {
            processInventory(entity.getOrderType(), detail);
        }

        entity.setOrderStatus("SUBMITTED");
        StockOrderJpaEntity saved = stockOrderRepository.save(entity);
        return toStockOrderDTO(saved);
    }

    private void processInventory(String orderType, StockOrderDetailJpaEntity detail) {
        Optional<InventoryJpaEntity> inventoryOpt = inventoryRepository.findByProductIdAndLocationCode(
                detail.getProductId(), detail.getLocationCode());

        if ("INBOUND".equalsIgnoreCase(orderType)) {
            InventoryJpaEntity inventory;
            if (inventoryOpt.isPresent()) {
                inventory = inventoryOpt.get();
                inventory.setQuantity(inventory.getQuantity() + detail.getQuantity());
                inventory.setLastCheckTime(LocalDateTime.now());
            } else {
                inventory = new InventoryJpaEntity();
                inventory.setInventoryId(System.currentTimeMillis() + (long)(Math.random() * 1000));
                inventory.setProductId(detail.getProductId());
                inventory.setLocationCode(detail.getLocationCode());
                inventory.setQuantity(detail.getQuantity());
                inventory.setLastCheckTime(LocalDateTime.now());
            }
            inventoryRepository.save(inventory);
            cacheInventorySummary(inventory.getProductId());

        } else if ("OUTBOUND".equalsIgnoreCase(orderType)) {
            InventoryJpaEntity inventory = inventoryOpt.orElseThrow(() -> 
                new BusinessException(ErrorCode.PARAM_ERROR, 
                    String.format("库存不足：产品%d在库位%s无库存", detail.getProductId(), detail.getLocationCode())));
            
            if (inventory.getQuantity() < detail.getQuantity()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    String.format("库存不足：产品%d在库位%s仅剩%d，需求%d", 
                        detail.getProductId(), detail.getLocationCode(), inventory.getQuantity(), detail.getQuantity()));
            }
            inventory.setQuantity(inventory.getQuantity() - detail.getQuantity());
            inventory.setLastCheckTime(LocalDateTime.now());
            inventoryRepository.save(inventory);
            cacheInventorySummary(inventory.getProductId());
        }
    }

    private StockOrderDTO toStockOrderDTO(StockOrderJpaEntity entity) {
        StockOrderDTO dto = new StockOrderDTO();
        dto.setOrderId(entity.getOrderId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setOrderType(entity.getOrderType());
        dto.setOrderStatus(entity.getOrderStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        
        // 加载明细
        List<StockOrderDetailJpaEntity> details = stockOrderDetailRepository.findByOrderId(entity.getOrderId());
        if (details != null && !details.isEmpty()) {
            dto.setItems(details.stream().map(d -> {
                StockOrderItemDTO item = new StockOrderItemDTO();
                item.setProductId(d.getProductId());
                item.setQuantity(d.getQuantity());
                item.setLocationCode(d.getLocationCode());
                return item;
            }).collect(Collectors.toList()));
        } else {
            dto.setItems(Collections.emptyList());
        }
        
        return dto;
    }

    private void cacheInventorySummary(Long productId) {
        String key = "inventory:summary:" + productId;
        // 注意：这里需要计算该产品在所有库位的总库存
        String value = String.valueOf(inventoryRepository.findByProductId(productId)
                .stream()
                .mapToInt(InventoryJpaEntity::getQuantity)
                .sum());
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));
    }
}

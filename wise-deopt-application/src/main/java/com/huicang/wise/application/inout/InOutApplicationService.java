package com.huicang.wise.application.inout;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inout.StockOrder;
import com.huicang.wise.domain.inout.StockOrderDetail;
import com.huicang.wise.domain.inout.StockOrderType;
import com.huicang.wise.domain.inout.StockOrderStatus;
import com.huicang.wise.domain.repository.inout.StockOrderRepository;
import com.huicang.wise.domain.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.domain.inventory.Inventory;
import com.huicang.wise.domain.repository.inventory.InventoryRepository;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.domain.repository.tag.ProductTagRepository;
import com.huicang.wise.domain.repository.user.UserRepository;
import com.huicang.wise.domain.repository.warehouse.WarehouseRepository;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InOutApplicationService {

    private final StockOrderRepository stockOrderRepository;
    private final StockOrderDetailRepository stockOrderDetailRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductTagRepository productTagRepository;
    private final com.huicang.wise.domain.repository.inventory.ProductRepository productRepository;
    private final com.huicang.wise.domain.repository.user.UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;

    public InOutApplicationService(StockOrderRepository stockOrderRepository,
                                   StockOrderDetailRepository stockOrderDetailRepository,
                                   InventoryRepository inventoryRepository,
                                   ProductTagRepository productTagRepository,
                                   com.huicang.wise.domain.repository.inventory.ProductRepository productRepository,
                                   com.huicang.wise.domain.repository.user.UserRepository userRepository,
                                   WarehouseRepository warehouseRepository) {
        this.stockOrderRepository = stockOrderRepository;
        this.stockOrderDetailRepository = stockOrderDetailRepository;
        this.inventoryRepository = inventoryRepository;
        this.productTagRepository = productTagRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public StockOrderDTO createStockOrder(StockOrderCreateRequest request) throws BusinessException {
        if (request.getOrderNo() == null || request.getOrderNo().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "出入库单号不能为空");
        }
        if (request.getWarehouseId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仓库ID不能为空");
        }
        if (!warehouseRepository.existsById(request.getWarehouseId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "仓库不存在");
        }
        
        StockOrder entity = new StockOrder();
        entity.setOrderId(System.currentTimeMillis());
        entity.setOrderNo(request.getOrderNo());
        entity.setWarehouseId(request.getWarehouseId());
        
        // 类型转换 String -> Short
        entity.setType(convertOrderType(request.getType()));
        
        // 状态转换 String -> Short
        entity.setStatus(convertOrderStatus(request.getStatus()));
        
        entity.setTotalItems(0);
        entity.setCreateBy(request.getCreateBy());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateBy(request.getCreateBy());
        entity.setUpdateTime(LocalDateTime.now());
        StockOrder saved = stockOrderRepository.save(entity);

        // 保存明细
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<StockOrderDetail> details = request.getItems().stream().map(item -> {
                StockOrderDetail detail = new StockOrderDetail();
                detail.setOrderId(saved.getOrderId());
                detail.setProductId(item.getProductId());
                detail.setQuantity(item.getQuantity());
                detail.setLocationCode(item.getLocationCode());
                // tagId 在初始创建时可能为空，但数据库要求非空（或默认值），这里暂时设置为 0L 或其他默认值，
                // 同时也需要确保 StockOrderDetail 实体定义中 tagId 允许为 null，
                // 如果数据库表结构强制非空，则必须提供一个有效值。
                // 假设之前修改实体定义未生效或未同步到数据库表结构，这里先尝试设置一个默认值 0L (假设存在或不校验外键)
                // 但根据之前的错误日志，存在外键约束，所以不能随便设 0L。
                // 如果实体已修改为 nullable = true，但数据库表仍是 NOT NULL，则会报错。
                // 暂时设置为 null，并依赖 JPA 更新 schema 或手动调整数据库。
                // 鉴于之前修改了实体注解 nullable = true，可能是 Hibernate 校验通过但数据库校验失败。
                // 让我们再次确认 StockOrderDetail 实体定义。
                detail.setTagId(null); 
                detail.setCreateBy(request.getCreateBy());
                detail.setCreateTime(LocalDateTime.now());
                return detail;
            }).collect(Collectors.toList());
            stockOrderDetailRepository.saveAll(details);
            
            // 更新主单总数
            saved.setTotalItems(details.size());
            stockOrderRepository.save(saved);
        }

        return toStockOrderDTO(saved);
    }
    
    private Short convertOrderType(String type) {
        if ("IN".equalsIgnoreCase(type)) {
            return StockOrderType.INBOUND.getCode().shortValue();
        } else if ("OUT".equalsIgnoreCase(type)) {
            return StockOrderType.OUTBOUND.getCode().shortValue();
        }
        return StockOrderType.INBOUND.getCode().shortValue(); // Default
    }

    private Short convertOrderStatus(String status) {
        if ("PENDING".equalsIgnoreCase(status)) {
            return StockOrderStatus.PENDING.getCode().shortValue();
        } else if ("PROCESSING".equalsIgnoreCase(status)) {
            return StockOrderStatus.APPROVED.getCode().shortValue(); // Map PROCESSING to APPROVED? Or just use PENDING
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            return StockOrderStatus.COMPLETED.getCode().shortValue();
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            return StockOrderStatus.CANCELLED.getCode().shortValue();
        }
        return StockOrderStatus.PENDING.getCode().shortValue(); // Default
    }

    @Cacheable(prefix = "stockorder", key = "#orderId", timeout = 1800)
    public StockOrderDTO getStockOrder(Long orderId) throws BusinessException {
        StockOrder entity = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));
        return toStockOrderDTO(entity);
    }

    public StockOrderPageDTO listStockOrders(Integer page, Integer size) {
        int pageIndex = page == null || page < 1 ? 0 : page - 1;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<StockOrder> result = stockOrderRepository.findAll(PageRequest.of(pageIndex, pageSize));
        StockOrderPageDTO dto = new StockOrderPageDTO();
        
        List<StockOrderDTO> content = result.getContent().stream().map(this::toStockOrderDTO).collect(Collectors.toList());
        dto.setContent(content);
        dto.setTotalElements(result.getTotalElements());
        
        dto.setNumber(result.getNumber());
        dto.setSize(result.getSize());
        dto.setTotalPages(result.getTotalPages());
        dto.setHasNext(result.hasNext());
        dto.setHasPrevious(result.hasPrevious());
        
        return dto;
    }

    @Transactional
    public StockOrderDTO submitStockOrder(Long orderId) throws BusinessException {
        StockOrder entity = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));
        
        if (entity.getStatus() != StockOrderStatus.PENDING.getCode().shortValue() && 
            entity.getStatus() != StockOrderStatus.REJECTED.getCode().shortValue()) {
             throw new BusinessException(ErrorCode.PARAM_ERROR, "只有待处理或已驳回的单据可以提交");
        }

        List<StockOrderDetail> details = stockOrderDetailRepository.findByOrderId(orderId);
        if (details.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单据无明细，无法提交");
        }

        entity.setStatus(StockOrderStatus.SUBMITTED.getCode().shortValue());
        entity.setSubmitTime(LocalDateTime.now());
        entity.setSubmitBy(entity.getCreateBy());
        entity.setUpdateBy(entity.getCreateBy());
        entity.setUpdateTime(LocalDateTime.now());
        StockOrder saved = stockOrderRepository.save(entity);
        return toStockOrderDTO(saved);
    }

    @Transactional
    public StockOrderDTO auditStockOrder(Long orderId, boolean approved, String reason) throws BusinessException {
        StockOrder entity = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));

        if (entity.getStatus() != StockOrderStatus.SUBMITTED.getCode().shortValue()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有待审核的单据可以进行审核");
        }

        if (approved) {
            List<StockOrderDetail> details = stockOrderDetailRepository.findByOrderId(orderId);
            for (StockOrderDetail detail : details) {
                processInventory(entity.getWarehouseId(), entity.getType(), detail);
            }
            entity.setStatus(StockOrderStatus.APPROVED.getCode().shortValue());
        } else {
            entity.setStatus(StockOrderStatus.REJECTED.getCode().shortValue());
        }
        
        entity.setRemark(reason); // Assuming remark can be used for reject reason
        entity.setUpdateBy(entity.getCreateBy()); // Should be auditor ID but using creator for now or need to pass operator
        entity.setUpdateTime(LocalDateTime.now());
        StockOrder saved = stockOrderRepository.save(entity);
        return toStockOrderDTO(saved);
    }

    @Transactional
    public StockOrderDTO withdrawStockOrder(Long orderId) throws BusinessException {
        StockOrder entity = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));

        if (entity.getStatus() != StockOrderStatus.SUBMITTED.getCode().shortValue()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有待审核的单据可以撤回");
        }

        entity.setStatus(StockOrderStatus.PENDING.getCode().shortValue());
        entity.setUpdateBy(entity.getCreateBy());
        entity.setUpdateTime(LocalDateTime.now());
        StockOrder saved = stockOrderRepository.save(entity);
        return toStockOrderDTO(saved);
    }

    @Transactional
    public StockOrderDTO addItem(Long orderId, StockOrderItemDTO item) throws BusinessException {
        StockOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));

        if (order.getStatus() != StockOrderStatus.PENDING.getCode().shortValue() && 
            order.getStatus() != StockOrderStatus.REJECTED.getCode().shortValue()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有待处理或已驳回的单据可以添加明细");
        }

        ProductTag tag = productTagRepository.findById(item.getTagId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "标签不存在"));

        StockOrderDetail detail = new StockOrderDetail();
        detail.setOrderId(orderId);
        detail.setTagId(item.getTagId());
        detail.setProductId(tag.getProductId());
        detail.setCreateBy(order.getCreateBy());
        detail.setCreateTime(LocalDateTime.now());
        stockOrderDetailRepository.save(detail);

        order.setTotalItems(order.getTotalItems() + 1);
        order.setUpdateBy(order.getCreateBy());
        order.setUpdateTime(LocalDateTime.now());
        stockOrderRepository.save(order);

        return toStockOrderDTO(order);
    }

    @Transactional
    public StockOrderDTO updateStockOrder(Long orderId, StockOrderCreateRequest request) throws BusinessException {
        StockOrder entity = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));

        if (entity.getStatus() != StockOrderStatus.PENDING.getCode().shortValue() && 
            entity.getStatus() != StockOrderStatus.REJECTED.getCode().shortValue()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有待处理或已驳回的单据可以修改");
        }

        if (request.getRemark() != null) {
            entity.setRemark(request.getRemark());
        }
        
        // Update other fields if necessary, e.g. orderNo if allowed (usually not)
        
        entity.setUpdateBy(request.getCreateBy()); // Reuse createBy as operator
        entity.setUpdateTime(LocalDateTime.now());
        StockOrder saved = stockOrderRepository.save(entity);
        return toStockOrderDTO(saved);
    }

    @Transactional
    public void removeItem(Long orderId, Long tagId) throws BusinessException {
        StockOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "出入库单不存在"));

        if (order.getStatus() != StockOrderStatus.PENDING.getCode().shortValue() && 
            order.getStatus() != StockOrderStatus.REJECTED.getCode().shortValue()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有待处理或已驳回的单据可以删除明细");
        }

        List<StockOrderDetail> details = stockOrderDetailRepository.findByOrderId(orderId);
        for (StockOrderDetail detail : details) {
            if (detail.getTagId().equals(tagId)) {
                stockOrderDetailRepository.delete(detail);
                order.setTotalItems(order.getTotalItems() - 1);
                order.setUpdateBy(order.getCreateBy());
                order.setUpdateTime(LocalDateTime.now());
                stockOrderRepository.save(order);
                return;
            }
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "明细不存在");
    }

    private void processInventory(Long warehouseId, Short orderType, StockOrderDetail detail) {
        Inventory inventory = inventoryRepository.findByWarehouseIdAndProductId(warehouseId, detail.getProductId())
                .orElse(null);
        int quantityChange = detail.getQuantity() != null ? detail.getQuantity() : 1;

        if (orderType == 0) { // 入库
            if (inventory != null) {
                inventory.setQuantity(inventory.getQuantity() + quantityChange);
                inventory.setUpdateTime(LocalDateTime.now());
            } else {
                inventory = new Inventory();
                // inventory.setInventoryId(System.currentTimeMillis() + (long)(Math.random() * 1000)); // Let DB handle ID
                inventory.setWarehouseId(warehouseId);
                inventory.setProductId(detail.getProductId());
                inventory.setQuantity(quantityChange);
                inventory.setLockedQuantity(0);
                inventory.setUpdateTime(LocalDateTime.now());
            }
            inventoryRepository.save(inventory);

        } else if (orderType == 1) { // 出库
            if (inventory == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    String.format("库存不足：产品%d在当前仓库无库存", detail.getProductId()));
            }
            
            if (inventory.getQuantity() < quantityChange) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, 
                    String.format("库存不足：产品%d库存为%d，需出库%d", 
                        detail.getProductId(), inventory.getQuantity(), quantityChange));
            }
            
            inventory.setQuantity(inventory.getQuantity() - quantityChange);
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryRepository.save(inventory);
        }
    }

    private StockOrderDTO toStockOrderDTO(StockOrder entity) {
        StockOrderDTO dto = new StockOrderDTO();
        dto.setOrderId(entity.getOrderId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setWarehouseId(entity.getWarehouseId());
        if (entity.getWarehouseId() != null) {
            warehouseRepository.findById(entity.getWarehouseId())
                .ifPresent(w -> dto.setWarehouseName(w.getWarehouseName()));
        }
        dto.setOrderType(entity.getType());
        dto.setOrderStatus(entity.getStatus());
        dto.setTotalItems(entity.getTotalItems());
        dto.setRemark(entity.getRemark());
        dto.setCreateTime(entity.getCreateTime());
        dto.setCreateBy(entity.getCreateBy());
        if (entity.getCreateBy() != null) {
            userRepository.findById(entity.getCreateBy()).ifPresent(user -> {
                dto.setCreatedByName(user.getUsername());
            });
        }
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setSubmitTime(entity.getSubmitTime());
        dto.setSubmitBy(entity.getSubmitBy());

        // Format Date
        if (entity.getCreateTime() != null) {
            dto.setCreatedAt(entity.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        // Map Type
        if (entity.getType() != null) {
            StockOrderType typeEnum = StockOrderType.fromCode(Integer.valueOf(entity.getType()));
            if (typeEnum == StockOrderType.INBOUND) {
                dto.setOrderTypeStr("IN");
            } else if (typeEnum == StockOrderType.OUTBOUND) {
                dto.setOrderTypeStr("OUT");
            }
        }

        // Map Status
        if (entity.getStatus() != null) {
            StockOrderStatus statusEnum = StockOrderStatus.fromCode(Integer.valueOf(entity.getStatus()));
            if (statusEnum != null) {
                // Return the enum name directly to align with client enum
                // PENDING, SUBMITTED, APPROVED, REJECTED, COMPLETED, CANCELLED
                dto.setOrderStatusStr(statusEnum.name());
            }
        }
        
        List<StockOrderDetail> details = stockOrderDetailRepository.findByOrderId(entity.getOrderId());
        if (details != null && !details.isEmpty()) {
            dto.setItems(details.stream().map(d -> {
                StockOrderItemDTO item = new StockOrderItemDTO();
                item.setTagId(d.getTagId());
                item.setProductId(d.getProductId());
                item.setQuantity(d.getQuantity());
                item.setLocationCode(d.getLocationCode());
                
                // Fetch Product Info
                 if (d.getProductId() != null) {
                     productRepository.findById(d.getProductId()).ifPresent(product -> {
                         item.setProductName(product.getName());
                         item.setProductCode(product.getCode());
                         item.setProductSpecification(product.getModel());
                     });
                 }
                
                return item;
            }).collect(Collectors.toList()));
        } else {
            dto.setItems(Collections.emptyList());
        }
        
        return dto;
    }
}

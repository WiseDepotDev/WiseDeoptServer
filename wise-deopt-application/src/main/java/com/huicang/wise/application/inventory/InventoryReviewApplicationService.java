package com.huicang.wise.application.inventory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.inventory.InventoryDifferenceJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryDifferenceRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;

/**
 * 类功能描述：库存差异复核应用服务
 *
 * @author xingchentye
 * @date 2026-01-25
 */
@Service
public class InventoryReviewApplicationService {

    private final InventoryDifferenceRepository inventoryDifferenceRepository;
    private final InventoryRepository inventoryRepository;

    public InventoryReviewApplicationService(InventoryDifferenceRepository inventoryDifferenceRepository,
                                             InventoryRepository inventoryRepository) {
        this.inventoryDifferenceRepository = inventoryDifferenceRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * 方法功能描述：获取差异列表
     *
     * @param status 状态过滤
     * @return 差异列表
     */
    public List<InventoryDifferenceDTO> listDifferences(Integer status) {
        List<InventoryDifferenceJpaEntity> entities;
        if (status != null) {
            entities = inventoryDifferenceRepository.findByStatus(status);
        } else {
            entities = inventoryDifferenceRepository.findAll();
        }
        return entities.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 方法功能描述：复核/处理差异
     *
     * @param diffId  差异ID
     * @param request 处理请求
     */
    @Transactional
    public void reviewDifference(Long diffId, ReviewDifferenceRequest request) {
        InventoryDifferenceJpaEntity diff = inventoryDifferenceRepository.findById(diffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "差异记录不存在"));

        if (diff.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该差异已处理");
        }

        String action = request.getAction();
        if ("CORRECT".equalsIgnoreCase(action)) {
            // 修正库存：将系统库存更新为实际盘点数量
            InventoryJpaEntity inventory = inventoryRepository.findByProductIdAndLocationCode(
                    diff.getProductId(), diff.getLocationCode())
                    .orElse(null);

            if (inventory != null) {
                inventory.setQuantity(diff.getActualQuantity());
                inventory.setLastCheckTime(LocalDateTime.now());
                inventoryRepository.save(inventory);
            } else {
                // 如果库存不存在且实际数量>0，则创建新库存
                if (diff.getActualQuantity() > 0) {
                    InventoryJpaEntity newInventory = new InventoryJpaEntity();
                    newInventory.setInventoryId(System.currentTimeMillis());
                    newInventory.setProductId(diff.getProductId());
                    newInventory.setLocationCode(diff.getLocationCode());
                    newInventory.setQuantity(diff.getActualQuantity());
                    newInventory.setLastCheckTime(LocalDateTime.now());
                    inventoryRepository.save(newInventory);
                }
            }
        }

        // 标记为已处理
        diff.setStatus(1);
        diff.setUpdatedAt(LocalDateTime.now());
        inventoryDifferenceRepository.save(diff);
    }

    /**
     * 方法功能描述：创建差异（模拟或由其他服务调用）
     */
    @Transactional
    public InventoryDifferenceDTO createDifference(InventoryDifferenceDTO dto) {
        InventoryDifferenceJpaEntity entity = new InventoryDifferenceJpaEntity();
        entity.setDiffId(System.currentTimeMillis());
        entity.setTaskId(dto.getTaskId());
        entity.setProductId(dto.getProductId());
        entity.setProductName(dto.getProductName());
        entity.setLocationCode(dto.getLocationCode());
        entity.setExpectedQuantity(dto.getExpectedQuantity());
        entity.setActualQuantity(dto.getActualQuantity());
        entity.setDiffType(dto.getDiffType());
        entity.setStatus(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        
        return toDTO(inventoryDifferenceRepository.save(entity));
    }

    private InventoryDifferenceDTO toDTO(InventoryDifferenceJpaEntity entity) {
        InventoryDifferenceDTO dto = new InventoryDifferenceDTO();
        dto.setDiffId(entity.getDiffId());
        dto.setTaskId(entity.getTaskId());
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setLocationCode(entity.getLocationCode());
        dto.setExpectedQuantity(entity.getExpectedQuantity());
        dto.setActualQuantity(entity.getActualQuantity());
        dto.setDiffType(entity.getDiffType());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

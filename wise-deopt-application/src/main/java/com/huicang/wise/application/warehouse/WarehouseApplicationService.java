package com.huicang.wise.application.warehouse;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.warehouse.Warehouse;
import com.huicang.wise.infrastructure.persistence.repository.warehouse.WarehouseRepository;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 仓库应用服务
 *
 * @author WiseDepot
 * @version 0.0.1
 * @since 2026-03-14
 */
@Service
@Transactional
public class WarehouseApplicationService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseApplicationService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    /**
     * 查询仓库列表
     *
     * @param keyword 关键字
     * @return 仓库列表
     */
    public List<WarehouseDTO> listWarehouses(String keyword) {
        List<Warehouse> warehouses = warehouseRepository.findByKeyword(keyword);
        return warehouses.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 创建仓库
     *
     * @param request 创建请求
     * @return 仓库DTO
     */
    @CacheEvict(prefix = "warehouse", allEntries = true)
    public WarehouseDTO createWarehouse(WarehouseCreateRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseName(request.getWarehouseName());
        warehouse.setWarehouseCode(request.getWarehouseCode());
        warehouse.setDescription(request.getDescription());
        warehouse.setAddress(request.getAddress());
        warehouse.setCreateTime(LocalDateTime.now());
        warehouse.setUpdateTime(LocalDateTime.now());

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return toDTO(savedWarehouse);
    }

    /**
     * 更新仓库
     *
     * @param id 仓库ID
     * @param request 更新请求
     * @return 仓库DTO
     */
    public WarehouseDTO updateWarehouse(Long id, WarehouseUpdateRequest request) {
        Warehouse warehouse =
                warehouseRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Warehouse not found with id: " + id));

        if (request.getWarehouseName() != null) {
            warehouse.setWarehouseName(request.getWarehouseName());
        }
        if (request.getWarehouseCode() != null) {
            warehouse.setWarehouseCode(request.getWarehouseCode());
        }
        if (request.getDescription() != null) {
            warehouse.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            warehouse.setAddress(request.getAddress());
        }
        warehouse.setUpdateTime(LocalDateTime.now());

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return toDTO(savedWarehouse);
    }

    /**
     * 删除仓库
     *
     * @param id 仓库ID
     */
    public void deleteWarehouse(Long id) {
        if (!warehouseRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Warehouse not found with id: " + id);
        }
        warehouseRepository.deleteById(id);
    }

    /**
     * 获取仓库详情
     *
     * @param id 仓库ID
     * @return 仓库DTO
     */
    @Cacheable(prefix = "warehouse", key = "#id", timeout = 3600)
    public WarehouseDTO getWarehouse(Long id) {
        Warehouse warehouse =
                warehouseRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "Warehouse not found with id: " + id));
        return toDTO(warehouse);
    }

    private WarehouseDTO toDTO(Warehouse warehouse) {
        WarehouseDTO dto = new WarehouseDTO();
        dto.setWarehouseId(warehouse.getWarehouseId());
        dto.setWarehouseName(warehouse.getWarehouseName());
        dto.setWarehouseCode(warehouse.getWarehouseCode());
        dto.setDescription(warehouse.getDescription());
        dto.setAddress(warehouse.getAddress());
        dto.setCreateTime(warehouse.getCreateTime());
        dto.setUpdateTime(warehouse.getUpdateTime());
        return dto;
    }
}

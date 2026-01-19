package com.huicang.wise.application.inventory;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.inventory.InventoryJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.repository.inventory.ProductJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类功能描述：库存应用服务
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现产品与库存用例编排
 */
@Service
public class InventoryApplicationService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public InventoryApplicationService(ProductRepository productRepository,
                                       InventoryRepository inventoryRepository,
                                       StringRedisTemplate stringRedisTemplate) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 方法功能描述：创建产品
     *
     * @param request 产品创建请求
     * @return 产品信息
     * @throws BusinessException 当产品编码为空时抛出异常
     */
    @Transactional
    public ProductDTO createProduct(ProductCreateRequest request) throws BusinessException {
        if (request.getProductCode() == null || request.getProductCode().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品编码不能为空");
        }
        if (request.getProductName() == null || request.getProductName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品名称不能为空");
        }
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setProductId(System.currentTimeMillis());
        entity.setProductCode(request.getProductCode());
        entity.setProductName(request.getProductName());
        entity.setModel(request.getModel());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        ProductJpaEntity saved = productRepository.save(entity);
        return toProductDTO(saved);
    }

    /**
     * 方法功能描述：更新产品
     *
     * @param productId 产品主键ID
     * @param request   产品更新请求
     * @return 产品信息
     * @throws BusinessException 当产品不存在时抛出异常
     */
    @Transactional
    public ProductDTO updateProduct(Long productId, ProductUpdateRequest request) throws BusinessException {
        ProductJpaEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在"));
        entity.setProductName(request.getProductName());
        entity.setModel(request.getModel());
        entity.setUpdatedAt(LocalDateTime.now());
        ProductJpaEntity saved = productRepository.save(entity);
        return toProductDTO(saved);
    }

    /**
     * 方法功能描述：获取产品信息
     *
     * @param productId 产品主键ID
     * @return 产品信息
     * @throws BusinessException 当产品不存在时抛出异常
     */
    public ProductDTO getProduct(Long productId) throws BusinessException {
        ProductJpaEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在"));
        return toProductDTO(entity);
    }

    /**
     * 方法功能描述：创建库存明细
     *
     * @param request 库存创建请求
     * @return 库存明细信息
     * @throws BusinessException 当产品不存在时抛出异常
     */
    @Transactional
    public InventoryDTO createInventory(InventoryCreateRequest request) throws BusinessException {
        if (request.getProductId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品ID不能为空");
        }
        if (request.getQuantity() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "库存数量不能为空");
        }
        if (productRepository.findById(request.getProductId()).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在");
        }
        InventoryJpaEntity entity = new InventoryJpaEntity();
        entity.setInventoryId(System.currentTimeMillis());
        entity.setProductId(request.getProductId());
        entity.setLocationCode(request.getLocationCode());
        entity.setQuantity(request.getQuantity());
        entity.setLastCheckTime(LocalDateTime.now());
        InventoryJpaEntity saved = inventoryRepository.save(entity);
        cacheInventorySummary(saved.getProductId());
        return toInventoryDTO(saved);
    }

    /**
     * 方法功能描述：更新库存明细
     *
     * @param inventoryId 库存明细ID
     * @param request     库存更新请求
     * @return 库存明细信息
     * @throws BusinessException 当库存不存在时抛出异常
     */
    @Transactional
    public InventoryDTO updateInventory(Long inventoryId, InventoryUpdateRequest request) throws BusinessException {
        InventoryJpaEntity entity = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "库存明细不存在"));
        entity.setLocationCode(request.getLocationCode());
        entity.setQuantity(request.getQuantity());
        entity.setLastCheckTime(LocalDateTime.now());
        InventoryJpaEntity saved = inventoryRepository.save(entity);
        cacheInventorySummary(saved.getProductId());
        return toInventoryDTO(saved);
    }

    /**
     * 方法功能描述：查询产品库存列表
     *
     * @param productId 产品主键ID
     * @return 库存明细列表
     */
    public List<InventoryDTO> listInventoryByProduct(Long productId) {
        List<InventoryJpaEntity> entities = inventoryRepository.findByProductId(productId);
        return entities.stream().map(this::toInventoryDTO).collect(Collectors.toList());
    }

    private ProductDTO toProductDTO(ProductJpaEntity entity) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(entity.getProductId());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setModel(entity.getModel());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private InventoryDTO toInventoryDTO(InventoryJpaEntity entity) {
        InventoryDTO dto = new InventoryDTO();
        dto.setInventoryId(entity.getInventoryId());
        dto.setProductId(entity.getProductId());
        dto.setLocationCode(entity.getLocationCode());
        dto.setQuantity(entity.getQuantity());
        dto.setLastCheckTime(entity.getLastCheckTime());
        return dto;
    }

    private void cacheInventorySummary(Long productId) {
        String key = "inventory:summary:" + productId;
        String value = String.valueOf(inventoryRepository.findByProductId(productId)
                .stream()
                .mapToInt(InventoryJpaEntity::getQuantity)
                .sum());
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));
    }
}

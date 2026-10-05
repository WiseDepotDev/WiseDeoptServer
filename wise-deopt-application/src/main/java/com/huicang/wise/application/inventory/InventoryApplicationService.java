package com.huicang.wise.application.inventory;

import com.huicang.wise.application.dashboard.DashboardKpiCache;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inventory.Inventory;
import com.huicang.wise.domain.inventory.Product;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.domain.warehouse.Warehouse;
import com.huicang.wise.infrastructure.persistence.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.TagRepository;
import com.huicang.wise.infrastructure.persistence.repository.warehouse.WarehouseRepository;
import com.huicang.wise.infrastructure.redis.RedisCacheUtils;
import com.huicang.wise.infrastructure.redis.RedisKeys;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 类功能描述：库存应用服务
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现产品与库存用例编排
 * @modified xingchentye 2026-02-27 优化分页查询，使用数据库级别分页
 * @modified xingchentye 2026-02-27 实现版本0.1.11功能：产品列表查询、库存锁定/解锁、库存预警、库存统计
 */
@Service
public class InventoryApplicationService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final TagRepository tagRepository;
    private final DashboardKpiCache dashboardKpiCache;

    public InventoryApplicationService(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            WarehouseRepository warehouseRepository,
            TagRepository tagRepository,
            DashboardKpiCache dashboardKpiCache) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.tagRepository = tagRepository;
        this.dashboardKpiCache = dashboardKpiCache;
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
        if (request.getProductName() == null || request.getProductName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品名称不能为空");
        }
        if (request.getProductCode() == null || request.getProductCode().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品编码不能为空");
        }
        Product entity = new Product();
        entity.setName(request.getProductName());
        entity.setCode(request.getProductCode());
        entity.setModel(request.getModel());
        entity.setUnit(request.getUnit());
        entity.setCreateTime(LocalDateTime.now());
        entity.setCreateBy(1L);
        entity.setUpdateTime(LocalDateTime.now());
        entity.setUpdateBy(1L);
        Product saved = productRepository.save(entity);
        return toProductDTO(saved);
    }

    /**
     * 方法功能描述：更新产品
     *
     * @param productId 产品主键ID
     * @param request 产品更新请求
     * @return 产品信息
     * @throws BusinessException 当产品不存在时抛出异常
     */
    @CacheEvict(prefix = "product", key = "#productId", allEntries = false)
    @Transactional
    public ProductDTO updateProduct(Long productId, ProductUpdateRequest request)
            throws BusinessException {
        Product entity =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在"));

        if (request.getProductName() != null && !request.getProductName().isBlank()) {
            entity.setName(request.getProductName());
        }

        if (request.getProductCode() != null && !request.getProductCode().isBlank()) {
            entity.setCode(request.getProductCode());
        } else if (entity.getCode() == null || entity.getCode().isBlank()) {
            // 修复旧数据缺失编码的问题
            entity.setCode("P" + System.currentTimeMillis());
        }

        if (request.getModel() != null) {
            entity.setModel(request.getModel());
        }

        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            entity.setUnit(request.getUnit());
        } else if (entity.getUnit() == null || entity.getUnit().isBlank()) {
            // 修复旧数据缺失单位的问题
            entity.setUnit("个");
        }

        entity.setUpdateTime(LocalDateTime.now());
        entity.setUpdateBy(1L);
        Product saved = productRepository.save(entity);
        return toProductDTO(saved);
    }

    /**
     * 方法功能描述：删除产品
     *
     * @param productId 产品主键ID
     * @throws BusinessException 当产品不存在时抛出异常
     */
    @Transactional
    public void deleteProduct(Long productId) throws BusinessException {
        Product entity =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在"));
        productRepository.delete(entity);
    }

    /**
     * 方法功能描述：获取产品信息
     *
     * @param productId 产品主键ID
     * @return 产品信息
     * @throws BusinessException 当产品不存在时抛出异常
     */
    @Cacheable(prefix = "product", key = "#productId", timeout = 3600)
    public ProductDTO getProduct(Long productId) throws BusinessException {
        Product entity =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产品不存在"));
        return toProductDTO(entity);
    }

    /**
     * 方法功能描述：查询产品列表（支持分页和筛选）
     *
     * @param name 产品名称（可选，支持模糊查询）
     * @param productCode 产品编码（可选，支持产品编码或条形码）
     * @param barcode 条形码（可选，已废弃，使用productCode参数）
     * @param categoryId 产品分类ID（可选，已废弃）
     * @param enabled 是否启用（可选，已废弃）
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页记录数（默认10）
     * @return 产品分页数据
     */
    public ProductPageDTO listProducts(
            String name,
            String productCode,
            String barcode,
            Long categoryId,
            Boolean enabled,
            Integer page,
            Integer pageSize) {
        int actualPage = page != null && page >= 1 ? page : 1;
        int actualPageSize = pageSize != null && pageSize > 0 ? pageSize : 10;
        int pageNum = actualPage - 1;

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(pageNum, actualPageSize, sort);

        Page<Product> pageResult;

        String searchCode = (barcode != null && !barcode.isBlank()) ? barcode : productCode;

        if (searchCode != null && !searchCode.isBlank()) {
            pageResult = findProductsByCodeOrBarcode(searchCode, pageable);
        } else {
            pageResult = productRepository.findProducts(name, pageable);
        }

        ProductPageDTO result = new ProductPageDTO();
        result.setTotal(pageResult.getTotalElements());
        result.setRows(
                pageResult.getContent().stream()
                        .map(this::toProductDTO)
                        .collect(Collectors.toList()));
        return result;
    }

    /**
     * 根据产品编码或条形码查询产品
     *
     * @param code 产品编码或条形码
     * @param pageable 分页参数
     * @return 产品分页结果
     */
    private Page<Product> findProductsByCodeOrBarcode(String code, Pageable pageable) {
        Page<Product> byCode = productRepository.findByCode(code, pageable);
        if (!byCode.isEmpty()) {
            return byCode;
        }

        ProductTag tag = tagRepository.findByBarcode(code).orElse(null);
        if (tag != null && tag.getProductId() != null) {
            Product product = productRepository.findById(tag.getProductId()).orElse(null);
            if (product != null) {
                List<Product> products = List.of(product);
                return new org.springframework.data.domain.PageImpl<>(products, pageable, 1);
            }
        }
        return Page.empty(pageable);
    }

    /**
     * 方法功能描述：创建库存明细
     *
     * @param request 库存创建请求
     * @return 库存明细信息
     * @throws BusinessException 当产品不存在时抛出异常
     */
    @CacheEvict(prefix = "inventory", key = "#request.productId", allEntries = false)
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
        Inventory entity = new Inventory();
        entity.setProductId(request.getProductId());
        entity.setQuantity(request.getQuantity());
        entity.setLockedQuantity(0);
        entity.setUpdateTime(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(entity);
        cacheInventorySummary(saved.getProductId());
        clearTotalInventoryCache();
        return toInventoryDTO(saved);
    }

    /**
     * 方法功能描述：更新库存明细
     *
     * @param inventoryId 库存明细ID
     * @param request 库存更新请求
     * @return 库存明细信息
     * @throws BusinessException 当库存不存在时抛出异常
     */
    @CacheEvict(prefix = "inventory", key = "#inventoryId", allEntries = false)
    @Transactional
    public InventoryDTO updateInventory(Long inventoryId, InventoryUpdateRequest request)
            throws BusinessException {
        Inventory entity =
                inventoryRepository
                        .findById(inventoryId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "库存明细不存在"));

        if (request.getQuantity() != null) {
            entity.setQuantity(request.getQuantity());
        }
        if (request.getWarehouseId() != null) {
            if (!warehouseRepository.existsById(request.getWarehouseId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "仓库不存在");
            }
            entity.setWarehouseId(request.getWarehouseId());
        }

        entity.setUpdateTime(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(entity);
        cacheInventorySummary(saved.getProductId());
        clearTotalInventoryCache();
        return toInventoryDTO(saved);
    }

    /**
     * 方法功能描述：查询库存明细详情
     *
     * @param inventoryId 库存明细ID
     * @return 库存明细信息
     * @throws BusinessException 当库存不存在时抛出异常
     */
    @Cacheable(prefix = "inventory", key = "#inventoryId", timeout = 1800)
    public InventoryDTO getInventory(Long inventoryId) throws BusinessException {
        Inventory entity =
                inventoryRepository
                        .findById(inventoryId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "库存明细不存在"));
        return toInventoryDTO(entity);
    }

    /**
     * 方法功能描述：删除库存明细
     *
     * @param inventoryId 库存明细ID
     * @throws BusinessException 当库存不存在时抛出异常
     */
    @CacheEvict(prefix = "inventory", key = "#inventoryId", allEntries = false)
    @Transactional
    public void deleteInventory(Long inventoryId) throws BusinessException {
        Inventory entity =
                inventoryRepository
                        .findById(inventoryId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "库存明细不存在"));
        inventoryRepository.delete(entity);
        cacheInventorySummary(entity.getProductId());
        clearTotalInventoryCache();
    }

    /**
     * 方法功能描述：查询产品库存列表
     *
     * @param productId 产品主键ID
     * @return 库存明细列表
     */
    public List<InventoryDTO> listInventoryByProduct(Long productId) {
        List<Inventory> entities = inventoryRepository.findAllByProductId(productId);
        return entities.stream().map(this::toInventoryDTO).collect(Collectors.toList());
    }

    /**
     * 方法功能描述：获取全部库存列表（强制分页）
     *
     * @param productId 产品主键ID（可选）
     * @param warehouseId 仓库ID（可选）
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页记录数（默认10）
     * @return 库存分页数据
     */
    public InventoryPageDTO listAllInventory(
            Long productId, Long warehouseId, Integer page, Integer pageSize) {
        int actualPage = page != null && page >= 1 ? page : 1;
        int actualPageSize = pageSize != null && pageSize > 0 ? pageSize : 10;
        int pageNum = actualPage - 1;

        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        Pageable pageable = PageRequest.of(pageNum, actualPageSize, sort);

        Page<Inventory> pageResult;
        if (productId != null && warehouseId != null) {
            pageResult =
                    inventoryRepository.findByProductIdAndWarehouseId(
                            productId, warehouseId, pageable);
        } else if (productId != null) {
            pageResult = inventoryRepository.findByProductId(productId, pageable);
        } else if (warehouseId != null) {
            pageResult = inventoryRepository.findByWarehouseId(warehouseId, pageable);
        } else {
            pageResult = inventoryRepository.findAll(pageable);
        }

        InventoryPageDTO result = new InventoryPageDTO();
        result.setTotal(pageResult.getTotalElements());
        result.setRows(
                pageResult.getContent().stream()
                        .map(this::toInventoryDTO)
                        .collect(Collectors.toList()));
        return result;
    }

    /**
     * 方法功能描述：搜索产品
     *
     * @param keyword 搜索关键字
     * @return 产品列表
     */
    public List<ProductDTO> searchProducts(String keyword) {
        Page<Product> pageResult =
                productRepository.findByNameContaining(keyword, PageRequest.of(0, 100));
        return pageResult.getContent().stream()
                .map(this::toProductDTO)
                .collect(Collectors.toList());
    }

    public List<InventoryDTO> searchInventoryByLocation(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        List<Inventory> inventories = inventoryRepository.findAll();
        List<Product> products =
                productRepository.findByNameContaining(keyword, Pageable.unpaged()).getContent();

        List<Long> productIds =
                products.stream().map(Product::getProductId).collect(Collectors.toList());

        return inventories.stream()
                .filter(inv -> productIds.contains(inv.getProductId()))
                .map(this::toInventoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 方法功能描述：锁定库存
     *
     * @param inventoryId 库存明细ID
     * @param quantity 锁定数量
     * @return 库存明细信息
     * @throws BusinessException 当库存不存在或可用数量不足时抛出异常
     */
    @Transactional
    public InventoryDTO lockInventory(Long inventoryId, Integer quantity) throws BusinessException {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "锁定数量必须大于0");
        }

        Inventory entity =
                inventoryRepository
                        .findById(inventoryId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "库存明细不存在"));

        if (entity.getQuantity() - entity.getLockedQuantity() < quantity) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "库存不足");
        }

        entity.setLockedQuantity(entity.getLockedQuantity() + quantity);
        entity.setUpdateTime(LocalDateTime.now());
        inventoryRepository.save(entity);
        cacheInventorySummary(entity.getProductId());
        clearTotalInventoryCache();
        return toInventoryDTO(entity);
    }

    /**
     * 方法功能描述：解锁库存
     *
     * @param inventoryId 库存明细ID
     * @param quantity 解锁数量
     * @return 库存明细信息
     * @throws BusinessException 当库存不存在或锁定数量不足时抛出异常
     */
    @Transactional
    public InventoryDTO unlockInventory(Long inventoryId, Integer quantity)
            throws BusinessException {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "解锁数量必须大于0");
        }

        Inventory entity =
                inventoryRepository
                        .findById(inventoryId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "库存明细不存在"));

        if (entity.getLockedQuantity() < quantity) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "锁定数量不足");
        }

        entity.setLockedQuantity(entity.getLockedQuantity() - quantity);
        entity.setUpdateTime(LocalDateTime.now());
        inventoryRepository.save(entity);
        cacheInventorySummary(entity.getProductId());
        clearTotalInventoryCache();
        return toInventoryDTO(entity);
    }

    /**
     * 方法功能描述：查询低库存预警
     *
     * @param threshold 阈值
     * @return 低库存列表
     */
    public List<InventoryDTO> getLowStockAlert(Integer threshold) {
        int finalThreshold = threshold != null ? threshold : 10;
        List<Inventory> entities = inventoryRepository.findAll();
        return entities.stream()
                .filter(inv -> inv.getQuantity() - inv.getLockedQuantity() < finalThreshold)
                .map(this::toInventoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 方法功能描述：查询即将过期的库存
     *
     * @param days 天数
     * @return 即将过期的库存列表
     */
    public List<InventoryDTO> getExpiringInventory(Integer days) {
        return List.of();
    }

    /**
     * 方法功能描述：统计库存总量
     *
     * @return 库存总量
     */
    @Cacheable(prefix = "inventory", key = "'total'", timeout = 300)
    public Integer getTotalInventoryQuantity() {
        Integer total = inventoryRepository.sumTotalQuantity();
        return total != null ? total : 0;
    }

    /**
     * 方法功能描述：按分类统计库存
     *
     * @return 分类库存统计
     */
    public List<Map<String, Object>> getInventoryByCategory() {
        return List.of();
    }

    /**
     * 方法功能描述：按位置统计库存
     *
     * @return 位置库存统计
     */
    public List<Map<String, Object>> getInventoryByLocation() {
        return List.of();
    }

    /**
     * 方法功能描述：获取产品库存统计
     *
     * @param productId 产品ID
     * @return 库存统计
     */
    public Map<String, Object> getProductInventoryStatistics(Long productId) {
        Map<String, Object> statistics = new HashMap<>();

        Integer totalQuantity = inventoryRepository.sumQuantityByProductId(productId);
        Integer lockedQuantity = inventoryRepository.sumLockedQuantityByProductId(productId);
        Integer availableQuantity =
                totalQuantity != null && lockedQuantity != null
                        ? totalQuantity - lockedQuantity
                        : 0;

        statistics.put("productId", productId);
        statistics.put("totalQuantity", totalQuantity != null ? totalQuantity : 0);
        statistics.put("lockedQuantity", lockedQuantity != null ? lockedQuantity : 0);
        statistics.put("availableQuantity", availableQuantity);

        return statistics;
    }

    private ProductDTO toProductDTO(Product entity) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getName());
        dto.setProductCode(entity.getCode());
        dto.setModel(entity.getModel());
        dto.setUnit(entity.getUnit());
        dto.setCreatedAt(entity.getCreateTime());
        dto.setUpdatedAt(entity.getUpdateTime());
        return dto;
    }

    /**
     * 转换为库存DTO
     *
     * @param inventory 库存实体
     * @return 库存DTO
     */
    private InventoryDTO toInventoryDTO(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setInventoryId(inventory.getInventoryId());
        dto.setProductId(inventory.getProductId());
        dto.setWarehouseId(inventory.getWarehouseId());
        // Inventory 实体没有 locationCode，且 DTO 也没有该字段，暂时忽略
        // dto.setLocationCode("");
        dto.setQuantity(inventory.getQuantity());

        // 关联产品信息
        if (inventory.getProductId() != null) {
            Optional<Product> productOpt = productRepository.findById(inventory.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                dto.setProductName(product.getName());
                dto.setProductCode(product.getCode());
                dto.setProductSpecification(product.getModel());
            }
        }

        // 关联仓库信息
        if (inventory.getWarehouseId() != null) {
            Optional<Warehouse> warehouseOpt =
                    warehouseRepository.findById(inventory.getWarehouseId());
            if (warehouseOpt.isPresent()) {
                dto.setWarehouseName(warehouseOpt.get().getWarehouseName());
            }
        }

        return dto;
    }

    private void cacheInventorySummary(Long productId) {
        String key = RedisKeys.inventorySummary(productId);
        Integer total = inventoryRepository.sumQuantityByProductId(productId);
        String value = total != null ? total.toString() : "0";
        RedisCacheUtils.set(key, value, 30, TimeUnit.MINUTES);
    }

    private void clearTotalInventoryCache() {
        String key = RedisKeys.INVENTORY_TOTAL;
        RedisCacheUtils.delete(key);

        // Also clear dashboard KPI cache to ensure data overview updates
        dashboardKpiCache.invalidate();
    }
}

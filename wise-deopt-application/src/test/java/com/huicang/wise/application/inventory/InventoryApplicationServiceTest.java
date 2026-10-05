package com.huicang.wise.application.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.dashboard.DashboardKpiCache;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inventory.Inventory;
import com.huicang.wise.domain.inventory.Product;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.infrastructure.persistence.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.TagRepository;
import com.huicang.wise.infrastructure.persistence.repository.warehouse.WarehouseRepository;
import com.huicang.wise.infrastructure.redis.RedisCacheManager;
import com.huicang.wise.infrastructure.redis.RedisCacheUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 库存应用服务的单元测试：产品 CRUD、库存 CRUD、锁定/解锁、预警与统计，以及缓存副作用。
 *
 * <p>覆盖要点：① 校验与"不存在"分支走的是不同错误码（前者 {@code PARAM_ERROR}，后者 {@code NOT_FOUND}）； ② 库存不足/锁定数量不足走的是
 * <b>{@code SYSTEM_ERROR}</b>（与出入库模块用 {@code PARAM_ERROR} 不一致，本测试把现状钉住）； ③ 写库存路径必须刷新 {@code
 * inventory:summary:<productId>} 并清 {@code inventory:total} 与 {@code dashboard:kpi} 两个键； ④ {@code
 * listProducts} 的分页兜底（page&lt;1→1、pageSize≤0→10）与"条码优先于产品编码"的取值顺序。
 */
@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

    private static final long PRODUCT_ID = 7L;
    private static final long INVENTORY_ID = 55L;
    private static final long WAREHOUSE_ID = 3L;

    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private TagRepository tagRepository;
    @Mock private DashboardKpiCache dashboardKpiCache;
    @Mock private RedisCacheManager cacheManager;

    private InventoryApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new InventoryApplicationService(
                        productRepository,
                        inventoryRepository,
                        warehouseRepository,
                        tagRepository,
                        dashboardKpiCache);
    }

    private Product product(String name, String code) {
        Product entity = new Product();
        entity.setName(name);
        entity.setCode(code);
        return entity;
    }

    private Inventory inventory(int quantity, int locked) {
        Inventory entity = new Inventory();
        entity.setQuantity(quantity);
        entity.setLockedQuantity(locked);
        return entity;
    }

    /** 只在真正会写缓存的用例里桩 Redis：避免 StrictStubs 报"多余桩"。 */
    private void stubRedis() {
        new RedisCacheUtils(cacheManager);
    }

    private ProductCreateRequest createRequest(String name, String code) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductName(name);
        request.setProductCode(code);
        return request;
    }

    private Product capturedProduct() {
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        return captor.getValue();
    }

    private Inventory capturedInventory() {
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------------- 产品 ----------------

    @Test
    @DisplayName("创建产品：名称为空被拒")
    void createProductRejectsBlankName() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.createProduct(createRequest("  ", "C1")));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("创建产品：编码为空被拒")
    void createProductRejectsBlankCode() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.createProduct(createRequest("螺丝", " ")));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("创建产品：成功落库并回填审计字段")
    void createProductSuccess() {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(service.createProduct(createRequest("螺丝", "C1")));

        Product saved = capturedProduct();
        assertEquals("螺丝", saved.getName());
        assertEquals("C1", saved.getCode());
        assertEquals(1L, saved.getCreateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("更新产品：不存在抛 NOT_FOUND")
    void updateProductMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateProduct(PRODUCT_ID, new ProductUpdateRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("更新产品：旧数据缺编码时用 P+时间戳 回填")
    void updateProductBackfillsMissingCode() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝", null)));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProduct(PRODUCT_ID, new ProductUpdateRequest());

        String code = capturedProduct().getCode();
        assertNotNull(code);
        assertTrue(code.startsWith("P"), "回填编码应以 P 开头，实际=" + code);
    }

    @Test
    @DisplayName("更新产品：旧数据缺单位时回填“个”")
    void updateProductBackfillsMissingUnit() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝", "C1")));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProduct(PRODUCT_ID, new ProductUpdateRequest());

        assertEquals("个", capturedProduct().getUnit());
    }

    @Test
    @DisplayName("更新产品：已有编码与单位不被空请求覆盖")
    void updateProductKeepsExistingValuesOnEmptyRequest() {
        Product existing = product("螺丝", "C1");
        existing.setUnit("盒");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProduct(PRODUCT_ID, new ProductUpdateRequest());

        Product saved = capturedProduct();
        assertEquals("C1", saved.getCode());
        assertEquals("盒", saved.getUnit());
    }

    @Test
    @DisplayName("删除产品：不存在抛 NOT_FOUND")
    void deleteProductMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deleteProduct(PRODUCT_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("删除产品：命中则删除实体")
    void deleteProductSuccess() {
        Product existing = product("螺丝", "C1");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));

        service.deleteProduct(PRODUCT_ID);

        verify(productRepository).delete(existing);
    }

    @Test
    @DisplayName("查询产品：不存在抛 NOT_FOUND")
    void getProductMissing() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getProduct(PRODUCT_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("查询产品：命中返回非空视图")
    void getProductFound() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝", "C1")));

        ProductDTO dto = service.getProduct(PRODUCT_ID);

        assertNotNull(dto);
        assertEquals("螺丝", dto.getProductName());
        assertEquals("C1", dto.getProductCode());
    }

    // ---------------- 产品列表 ----------------

    @Test
    @DisplayName("产品列表：page<1 与 pageSize<=0 兜底为第 1 页 10 条")
    void listProductsDefaultsPaging() {
        when(productRepository.findProducts(any(), any())).thenReturn(Page.empty());

        service.listProducts(null, null, null, null, null, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findProducts(any(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("产品列表：页码从 1 开始换算为 0 基")
    void listProductsConvertsPageNumber() {
        when(productRepository.findProducts(any(), any())).thenReturn(Page.empty());

        service.listProducts(null, null, null, null, null, 3, 25);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findProducts(any(), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(25, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("产品列表：条码优先于产品编码")
    void listProductsPrefersBarcode() {
        when(productRepository.findByCode(eq("BC"), any())).thenReturn(Page.empty());
        when(tagRepository.findByBarcode("BC")).thenReturn(Optional.empty());

        service.listProducts(null, "C1", "BC", null, null, 1, 10);

        verify(productRepository).findByCode(eq("BC"), any());
        verify(productRepository, never()).findProducts(any(), any());
    }

    @Test
    @DisplayName("产品列表：无编码与条码时按名称查询")
    void listProductsFallsBackToNameQuery() {
        when(productRepository.findProducts(eq("螺丝"), any()))
                .thenReturn(new PageImpl<>(List.of(product("螺丝", "C1")), PageRequest.of(0, 10), 1));

        ProductPageDTO result = service.listProducts("螺丝", null, null, null, null, 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRows().size());
    }

    @Test
    @DisplayName("产品列表：编码命中直接返回，不查条码标签")
    void listProductsCodeHitSkipsTags() {
        when(productRepository.findByCode(eq("C1"), any()))
                .thenReturn(new PageImpl<>(List.of(product("螺丝", "C1")), PageRequest.of(0, 10), 1));

        ProductPageDTO result = service.listProducts(null, "C1", null, null, null, 1, 10);

        assertEquals(1L, result.getTotal());
        verify(tagRepository, never()).findByBarcode(any());
    }

    @Test
    @DisplayName("产品列表：编码未命中时按条码标签反查产品")
    void listProductsFallsBackToBarcodeTag() {
        ProductTag tag = new ProductTag();
        tag.setProductId(99L);
        when(productRepository.findByCode(eq("BC"), any())).thenReturn(Page.empty());
        when(tagRepository.findByBarcode("BC")).thenReturn(Optional.of(tag));
        when(productRepository.findById(99L)).thenReturn(Optional.of(product("螺丝", "C1")));

        ProductPageDTO result = service.listProducts(null, null, "BC", null, null, 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRows().size());
    }

    @Test
    @DisplayName("产品列表：条码标签指向的产品不存在时返回空页")
    void listProductsBarcodeTagWithoutProduct() {
        ProductTag tag = new ProductTag();
        tag.setProductId(99L);
        when(productRepository.findByCode(eq("BC"), any())).thenReturn(Page.empty());
        when(tagRepository.findByBarcode("BC")).thenReturn(Optional.of(tag));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ProductPageDTO result = service.listProducts(null, null, "BC", null, null, 1, 10);

        assertEquals(0L, result.getTotal());
    }

    @Test
    @DisplayName("搜索产品：按名称模糊查询并映射")
    void searchProductsMapsResult() {
        when(productRepository.findByNameContaining(eq("螺"), any()))
                .thenReturn(
                        new PageImpl<>(List.of(product("螺丝", "C1")), PageRequest.of(0, 100), 1));

        List<ProductDTO> rows = service.searchProducts("螺");

        assertEquals(1, rows.size());
        assertEquals("螺丝", rows.get(0).getProductName());
    }

    // ---------------- 库存 ----------------

    @Test
    @DisplayName("创建库存：产品ID为空被拒")
    void createInventoryRejectsNullProductId() {
        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setQuantity(10);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createInventory(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("创建库存：数量为空被拒")
    void createInventoryRejectsNullQuantity() {
        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId(PRODUCT_ID);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createInventory(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("创建库存：产品不存在抛 NOT_FOUND")
    void createInventoryRejectsMissingProduct() {
        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(10);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createInventory(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("创建库存：锁定数量初始为 0，并刷新汇总缓存与总览缓存")
    void createInventoryWritesCache() {
        stubRedis();
        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(10);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝", "C1")));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.sumQuantityByProductId(PRODUCT_ID)).thenReturn(10);

        assertNotNull(service.createInventory(request));

        assertEquals(0, capturedInventory().getLockedQuantity());
        verify(cacheManager).set("inventory:summary:7", "10", 30L, TimeUnit.MINUTES);
        verify(cacheManager).delete("inventory:total");
        verify(dashboardKpiCache).invalidate();
    }

    @Test
    @DisplayName("更新库存：明细不存在抛 NOT_FOUND")
    void updateInventoryMissing() {
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateInventory(INVENTORY_ID, new InventoryUpdateRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("更新库存：目标仓库不存在抛 NOT_FOUND 且不落库")
    void updateInventoryRejectsMissingWarehouse() {
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.of(inventory(1, 0)));
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setWarehouseId(WAREHOUSE_ID);
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(false);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateInventory(INVENTORY_ID, request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("更新库存：数量与仓库被写入")
    void updateInventorySuccess() {
        stubRedis();
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.of(inventory(1, 0)));
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setQuantity(50);
        request.setWarehouseId(WAREHOUSE_ID);
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.sumQuantityByProductId(null)).thenReturn(null);

        assertNotNull(service.updateInventory(INVENTORY_ID, request));

        Inventory saved = capturedInventory();
        assertEquals(50, saved.getQuantity());
        assertEquals(WAREHOUSE_ID, saved.getWarehouseId());
    }

    @Test
    @DisplayName("查询库存：不存在抛 NOT_FOUND")
    void getInventoryMissing() {
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getInventory(INVENTORY_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("删除库存：命中则删除并刷新缓存")
    void deleteInventorySuccess() {
        stubRedis();
        Inventory existing = inventory(4, 0);
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.of(existing));
        when(inventoryRepository.sumQuantityByProductId(null)).thenReturn(null);

        service.deleteInventory(INVENTORY_ID);

        verify(inventoryRepository).delete(existing);
        verify(cacheManager).set("inventory:summary:null", "0", 30L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("按产品查库存列表：逐条映射")
    void listInventoryByProductMapsAll() {
        when(inventoryRepository.findAllByProductId(PRODUCT_ID))
                .thenReturn(List.of(inventory(1, 0), inventory(2, 0)));

        assertEquals(2, service.listInventoryByProduct(PRODUCT_ID).size());
    }

    @Test
    @DisplayName("库存分页：产品与仓库都给定时用组合条件")
    void listAllInventoryUsesBothConditions() {
        when(inventoryRepository.findByProductIdAndWarehouseId(
                        eq(PRODUCT_ID), eq(WAREHOUSE_ID), any()))
                .thenReturn(Page.empty());

        InventoryPageDTO result = service.listAllInventory(PRODUCT_ID, WAREHOUSE_ID, 1, 10);

        assertEquals(0L, result.getTotal());
        verify(inventoryRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("库存分页：两个条件都不给时走全量分页并兜底 pageSize")
    void listAllInventoryFallsBackToFindAll() {
        when(inventoryRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.listAllInventory(null, null, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(inventoryRepository).findAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("库存分页：只给仓库时按仓库分页")
    void listAllInventoryByWarehouseOnly() {
        when(inventoryRepository.findByWarehouseId(eq(WAREHOUSE_ID), any()))
                .thenReturn(Page.empty());

        assertNotNull(service.listAllInventory(null, WAREHOUSE_ID, 1, 10));

        verify(inventoryRepository).findByWarehouseId(eq(WAREHOUSE_ID), any());
        verify(inventoryRepository, never()).findByProductId(eq(PRODUCT_ID), any());
    }

    @Test
    @DisplayName("按位置搜索库存：关键字为空直接返回空表")
    void searchInventoryByLocationBlankKeyword() {
        assertEquals(0, service.searchInventoryByLocation("  ").size());
        verify(inventoryRepository, never()).findAll();
    }

    @Test
    @DisplayName("按位置搜索库存：按产品名命中过滤")
    void searchInventoryByLocationFiltersByProductName() {
        Product matched = product("螺丝", "C1");
        matched.setProductId(99L);
        Inventory hit = inventory(5, 0);
        hit.setProductId(99L);
        Inventory miss = inventory(6, 0);
        miss.setProductId(100L);
        when(inventoryRepository.findAll()).thenReturn(List.of(hit, miss));
        when(productRepository.findByNameContaining(eq("螺"), any()))
                .thenReturn(new PageImpl<>(List.of(matched)));

        List<InventoryDTO> rows = service.searchInventoryByLocation("螺");

        assertEquals(1, rows.size());
        assertEquals(5, rows.get(0).getQuantity());
    }

    // ---------------- 锁定 / 解锁 ----------------

    @Test
    @DisplayName("锁定库存：数量非正被拒")
    void lockInventoryRejectsNonPositiveQuantity() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.lockInventory(INVENTORY_ID, 0));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(inventoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("锁定库存：明细不存在抛 NOT_FOUND")
    void lockInventoryMissing() {
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.lockInventory(INVENTORY_ID, 1));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("锁定库存：可用量不足抛 SYSTEM_ERROR（与出入库模块的 PARAM_ERROR 不一致）")
    void lockInventoryInsufficientAvailable() {
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.of(inventory(10, 8)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.lockInventory(INVENTORY_ID, 5));

        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("锁定库存：成功则累加锁定数量并刷缓存")
    void lockInventoryAccumulatesLocked() {
        stubRedis();
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.of(inventory(10, 2)));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.sumQuantityByProductId(null)).thenReturn(null);

        assertNotNull(service.lockInventory(INVENTORY_ID, 3));

        assertEquals(5, capturedInventory().getLockedQuantity());
    }

    @Test
    @DisplayName("解锁库存：数量非正被拒")
    void unlockInventoryRejectsNonPositiveQuantity() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.unlockInventory(INVENTORY_ID, -1));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("解锁库存：锁定数量不足抛 SYSTEM_ERROR")
    void unlockInventoryInsufficientLocked() {
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.of(inventory(10, 1)));

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.unlockInventory(INVENTORY_ID, 2));

        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("解锁库存：成功则扣减锁定数量并刷缓存")
    void unlockInventoryReducesLocked() {
        stubRedis();
        when(inventoryRepository.findById(INVENTORY_ID)).thenReturn(Optional.of(inventory(10, 5)));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.sumQuantityByProductId(null)).thenReturn(null);

        assertNotNull(service.unlockInventory(INVENTORY_ID, 2));

        assertEquals(3, capturedInventory().getLockedQuantity());
    }

    // ---------------- 预警 / 统计 ----------------

    @Test
    @DisplayName("低库存预警：阈值缺省为 10，按可用量判定")
    void getLowStockAlertDefaultsThreshold() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory(5, 1), inventory(100, 0)));

        List<InventoryDTO> rows = service.getLowStockAlert(null);

        assertEquals(1, rows.size());
        assertEquals(5, rows.get(0).getQuantity());
    }

    @Test
    @DisplayName("低库存预警：显式阈值生效")
    void getLowStockAlertHonoursThreshold() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory(5, 1)));

        assertEquals(0, service.getLowStockAlert(3).size());
        assertEquals(1, service.getLowStockAlert(5).size());
    }

    @Test
    @DisplayName("库存总量：求和为空时回落到 0")
    void getTotalInventoryQuantityHandlesNull() {
        when(inventoryRepository.sumTotalQuantity()).thenReturn(null);

        assertEquals(0, service.getTotalInventoryQuantity());
    }

    @Test
    @DisplayName("库存总量：返回求和值")
    void getTotalInventoryQuantityReturnsSum() {
        when(inventoryRepository.sumTotalQuantity()).thenReturn(42);

        assertEquals(42, service.getTotalInventoryQuantity());
    }

    @Test
    @DisplayName("产品库存统计：可用量 = 总量 - 锁定量")
    void getProductInventoryStatisticsComputesAvailable() {
        when(inventoryRepository.sumQuantityByProductId(PRODUCT_ID)).thenReturn(10);
        when(inventoryRepository.sumLockedQuantityByProductId(PRODUCT_ID)).thenReturn(4);

        Map<String, Object> statistics = service.getProductInventoryStatistics(PRODUCT_ID);

        assertEquals(10, statistics.get("totalQuantity"));
        assertEquals(4, statistics.get("lockedQuantity"));
        assertEquals(6, statistics.get("availableQuantity"));
        assertEquals(PRODUCT_ID, statistics.get("productId"));
    }

    @Test
    @DisplayName("产品库存统计：求和为空时总量/锁定量记 0")
    void getProductInventoryStatisticsHandlesNull() {
        when(inventoryRepository.sumQuantityByProductId(PRODUCT_ID)).thenReturn(null);
        when(inventoryRepository.sumLockedQuantityByProductId(PRODUCT_ID)).thenReturn(null);

        Map<String, Object> statistics = service.getProductInventoryStatistics(PRODUCT_ID);

        assertEquals(0, statistics.get("totalQuantity"));
        assertEquals(0, statistics.get("lockedQuantity"));
        assertEquals(0, statistics.get("availableQuantity"));
    }

    // ---------------- 尚未实现（钉住现状，属假数据） ----------------

    @Test
    @DisplayName("现状固定：即将过期库存、分类统计、位置统计三处都直接返回空表（未实现）")
    void unimplementedQueriesReturnEmptyLists() {
        assertEquals(0, service.getExpiringInventory(7).size());
        assertEquals(0, service.getInventoryByCategory().size());
        assertEquals(0, service.getInventoryByLocation().size());
    }
}

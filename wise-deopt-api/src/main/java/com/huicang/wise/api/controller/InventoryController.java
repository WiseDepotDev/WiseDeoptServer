package com.huicang.wise.api.controller;

import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.inventory.InventoryCreateRequest;
import com.huicang.wise.application.inventory.InventoryDTO;
import com.huicang.wise.application.inventory.InventoryPageDTO;
import com.huicang.wise.application.inventory.InventoryUpdateRequest;
import com.huicang.wise.application.inventory.ProductCreateRequest;
import com.huicang.wise.application.inventory.ProductDTO;
import com.huicang.wise.application.inventory.ProductPageDTO;
import com.huicang.wise.application.inventory.ProductUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类功能描述：库存管理控制层
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现产品与库存接口
 * @modified xingchentye 2026-02-27 实现版本0.1.11功能：产品列表查询、库存锁定/解锁、库存预警、库存统计
 */
@Tag(name = "库存管理接口")
@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryApplicationService inventoryApplicationService;

    public InventoryController(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    /**
     * 方法功能描述：创建产品
     *
     * @param request 产品创建请求
     * @return 产品信息
     */
    @Operation(summary = "创建产品", description = "创建产品信息。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.PRODUCT_CREATE)
    @PostMapping("/products")
    public ApiResponse<ProductDTO> createProduct(
            @Parameter(description = "产品创建请求", required = true) @RequestBody
                    ProductCreateRequest request) {
        return ApiResponse.success(inventoryApplicationService.createProduct(request));
    }

    /**
     * 方法功能描述：更新产品
     *
     * @param productId 产品主键ID
     * @param request 产品更新请求
     * @return 产品信息
     */
    @Operation(summary = "更新产品", description = "更新产品信息。成功返回200；产品不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.PRODUCT_UPDATE)
    @PutMapping("/products/{productId}")
    public ApiResponse<ProductDTO> updateProduct(
            @Parameter(description = "产品主键ID", required = true) @PathVariable("productId")
                    Long productId,
            @Parameter(description = "产品更新请求", required = true) @RequestBody
                    ProductUpdateRequest request) {
        return ApiResponse.success(inventoryApplicationService.updateProduct(productId, request));
    }

    /**
     * 方法功能描述：删除产品
     *
     * @param productId 产品主键ID
     * @return 无
     */
    @Operation(summary = "删除产品", description = "删除产品信息。成功返回200；产品不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.PRODUCT_DELETE)
    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> deleteProduct(
            @Parameter(description = "产品主键ID", required = true) @PathVariable("productId")
                    Long productId) {
        inventoryApplicationService.deleteProduct(productId);
        return ApiResponse.success(null);
    }

    /**
     * 方法功能描述：查询产品详情
     *
     * @param productId 产品主键ID
     * @return 产品信息
     */
    @Operation(summary = "查询产品详情", description = "查询产品详情信息。成功返回200；产品不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.PRODUCT_DETAIL)
    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDTO> getProduct(
            @Parameter(description = "产品主键ID", required = true) @PathVariable("productId")
                    Long productId) {
        return ApiResponse.success(inventoryApplicationService.getProduct(productId));
    }

    /**
     * 方法功能描述：查询产品列表（支持分页和筛选）
     *
     * @param name 产品名称（可选，支持模糊查询）
     * @param categoryId 产品分类ID（可选）
     * @param enabled 是否启用（可选）
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页记录数（默认10）
     * @return 产品分页数据
     */
    @Operation(
            summary = "查询产品列表",
            description = "查询产品列表，支持按名称、产品编码、条形码、分类、状态筛选和分页查询。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.PRODUCT_LIST)
    @GetMapping("/products")
    public ApiResponse<ProductPageDTO> listProducts(
            @Parameter(description = "产品名称（可选，支持模糊查询）", required = false)
                    @RequestParam(value = "name", required = false)
                    String name,
            @Parameter(description = "产品编码（可选）", required = false)
                    @RequestParam(value = "productCode", required = false)
                    String productCode,
            @Parameter(description = "条形码（可选）", required = false)
                    @RequestParam(value = "barcode", required = false)
                    String barcode,
            @Parameter(description = "产品分类ID（可选）", required = false)
                    @RequestParam(value = "categoryId", required = false)
                    Long categoryId,
            @Parameter(description = "是否启用（可选）", required = false)
                    @RequestParam(value = "enabled", required = false)
                    Boolean enabled,
            @Parameter(description = "页码（从1开始，默认1）", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页记录数（默认10）", required = false)
                    @RequestParam(value = "pageSize", required = false)
                    Integer pageSize) {
        return ApiResponse.success(
                inventoryApplicationService.listProducts(
                        name, productCode, barcode, categoryId, enabled, page, pageSize));
    }

    /**
     * 方法功能描述：创建库存明细
     *
     * @param request 库存创建请求
     * @return 库存明细信息
     */
    @Operation(summary = "创建库存明细", description = "创建库存明细。成功返回200；参数错误返回400；产品不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_CREATE)
    @PostMapping
    public ApiResponse<InventoryDTO> createInventory(
            @Parameter(description = "库存创建请求", required = true) @RequestBody
                    InventoryCreateRequest request) {
        return ApiResponse.success(inventoryApplicationService.createInventory(request));
    }

    /**
     * 方法功能描述：查询库存明细详情
     *
     * @param inventoryId 库存明细ID
     * @return 库存明细信息
     */
    @Operation(summary = "查询库存明细详情", description = "查询库存明细详情信息。成功返回200；库存不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_DETAIL)
    @GetMapping("/{inventoryId}")
    public ApiResponse<InventoryDTO> getInventory(
            @Parameter(description = "库存明细ID", required = true) @PathVariable("inventoryId")
                    Long inventoryId) {
        return ApiResponse.success(inventoryApplicationService.getInventory(inventoryId));
    }

    /**
     * 方法功能描述：删除库存明细
     *
     * @param inventoryId 库存明细ID
     * @return 无
     */
    @Operation(summary = "删除库存明细", description = "删除库存明细。成功返回200；库存不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_DELETE)
    @DeleteMapping("/{inventoryId}")
    public ApiResponse<Void> deleteInventory(
            @Parameter(description = "库存明细ID", required = true) @PathVariable("inventoryId")
                    Long inventoryId) {
        inventoryApplicationService.deleteInventory(inventoryId);
        return ApiResponse.success(null);
    }

    /**
     * 方法功能描述：更新库存明细
     *
     * @param inventoryId 库存明细ID
     * @param request 库存更新请求
     * @return 库存明细信息
     */
    @Operation(summary = "更新库存明细", description = "更新库存明细。成功返回200；库存不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_UPDATE)
    @PutMapping("/{inventoryId}")
    public ApiResponse<InventoryDTO> updateInventory(
            @Parameter(description = "库存明细ID", required = true) @PathVariable("inventoryId")
                    Long inventoryId,
            @Parameter(description = "库存更新请求", required = true) @RequestBody
                    InventoryUpdateRequest request) {
        return ApiResponse.success(
                inventoryApplicationService.updateInventory(inventoryId, request));
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
    @Operation(
            summary = "获取全部库存列表",
            description = "获取全部库存列表，支持按产品ID、仓库ID过滤和分页查询。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_LIST_ALL)
    @GetMapping("/all")
    public ApiResponse<InventoryPageDTO> listAllInventory(
            @Parameter(description = "产品主键ID（可选）", required = false)
                    @RequestParam(value = "productId", required = false)
                    Long productId,
            @Parameter(description = "仓库ID（可选）", required = false)
                    @RequestParam(value = "warehouseId", required = false)
                    Long warehouseId,
            @Parameter(description = "页码（从1开始，默认1）", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页记录数（默认10）", required = false)
                    @RequestParam(value = "pageSize", required = false)
                    Integer pageSize) {
        return ApiResponse.success(
                inventoryApplicationService.listAllInventory(
                        productId, warehouseId, page, pageSize));
    }

    /**
     * 方法功能描述：按条件查询库存列表（强制分页）
     *
     * @param productId 产品主键ID（可选）
     * @param warehouseId 仓库ID（可选）
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页记录数（默认10）
     * @return 库存分页数据
     */
    @Operation(summary = "按条件查询库存", description = "根据产品ID或仓库ID查询库存列表，支持分页。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_LIST)
    @GetMapping
    public ApiResponse<InventoryPageDTO> listInventory(
            @Parameter(description = "产品主键ID（可选）", required = false)
                    @RequestParam(value = "productId", required = false)
                    Long productId,
            @Parameter(description = "仓库ID（可选）", required = false)
                    @RequestParam(value = "warehouseId", required = false)
                    Long warehouseId,
            @Parameter(description = "页码（从1开始，默认1）", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页记录数（默认10）", required = false)
                    @RequestParam(value = "pageSize", required = false)
                    Integer pageSize) {
        return ApiResponse.success(
                inventoryApplicationService.listAllInventory(
                        productId, warehouseId, page, pageSize));
    }

    /**
     * 方法功能描述：搜索库存
     *
     * @param keyword 搜索关键字
     * @param type 搜索类型（PRODUCT/LOCATION）
     * @return 搜索结果
     */
    @Operation(
            summary = "搜索库存",
            description = "搜索产品或按库位搜索库存。type=PRODUCT返回产品列表，type=LOCATION返回库存列表。")
    @ApiPacketType(PacketType.INVENTORY_SEARCH)
    @GetMapping("/search")
    public ApiResponse<Object> search(
            @Parameter(description = "搜索关键字", required = true) @RequestParam("keyword")
                    String keyword,
            @Parameter(description = "搜索类型(PRODUCT/LOCATION)", required = true)
                    @RequestParam("type")
                    String type) {
        if ("PRODUCT".equalsIgnoreCase(type)) {
            return ApiResponse.success(inventoryApplicationService.searchProducts(keyword));
        } else if ("LOCATION".equalsIgnoreCase(type)) {
            return ApiResponse.success(
                    inventoryApplicationService.searchInventoryByLocation(keyword));
        }
        return ApiResponse.success(List.of());
    }

    /**
     * 方法功能描述：锁定库存
     *
     * @param inventoryId 库存明细ID
     * @param quantity 锁定数量
     * @return 库存明细信息
     */
    @Operation(summary = "锁定库存", description = "锁定指定数量的库存。成功返回200；库存不足返回400；库存不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_LOCK)
    @PostMapping("/{inventoryId}/lock")
    public ApiResponse<InventoryDTO> lockInventory(
            @Parameter(description = "库存明细ID", required = true) @PathVariable("inventoryId")
                    Long inventoryId,
            @Parameter(description = "锁定数量", required = true) @RequestParam("quantity")
                    Integer quantity) {
        return ApiResponse.success(
                inventoryApplicationService.lockInventory(inventoryId, quantity));
    }

    /**
     * 方法功能描述：解锁库存
     *
     * @param inventoryId 库存明细ID
     * @param quantity 解锁数量
     * @return 库存明细信息
     */
    @Operation(
            summary = "解锁库存",
            description = "解锁指定数量的库存。成功返回200；锁定数量不足返回400；库存不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_UNLOCK)
    @PostMapping("/{inventoryId}/unlock")
    public ApiResponse<InventoryDTO> unlockInventory(
            @Parameter(description = "库存明细ID", required = true) @PathVariable("inventoryId")
                    Long inventoryId,
            @Parameter(description = "解锁数量", required = true) @RequestParam("quantity")
                    Integer quantity) {
        return ApiResponse.success(
                inventoryApplicationService.unlockInventory(inventoryId, quantity));
    }

    /**
     * 方法功能描述：查询低库存预警
     *
     * @param threshold 阈值（默认10）
     * @return 低库存列表
     */
    @Operation(summary = "查询低库存预警", description = "查询库存数量低于阈值的库存明细。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_LOW_STOCK)
    @GetMapping("/alerts/low-stock")
    public ApiResponse<List<InventoryDTO>> getLowStockAlert(
            @Parameter(description = "阈值（默认10）", required = false)
                    @RequestParam(value = "threshold", required = false)
                    Integer threshold) {
        return ApiResponse.success(inventoryApplicationService.getLowStockAlert(threshold));
    }

    /**
     * 方法功能描述：查询即将过期的库存
     *
     * @param days 天数（默认30）
     * @return 即将过期的库存列表
     */
    @Operation(summary = "查询即将过期的库存", description = "查询指定天数内即将过期的库存明细。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_EXPIRING)
    @GetMapping("/alerts/expiring")
    public ApiResponse<List<InventoryDTO>> getExpiringInventory(
            @Parameter(description = "天数（默认30）", required = false)
                    @RequestParam(value = "days", required = false)
                    Integer days) {
        return ApiResponse.success(inventoryApplicationService.getExpiringInventory(days));
    }

    /**
     * 方法功能描述：统计库存总量
     *
     * @return 库存总量
     */
    @Operation(summary = "统计库存总量", description = "统计所有产品的库存总量。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_TOTAL)
    @GetMapping("/statistics/total")
    public ApiResponse<Integer> getTotalInventoryQuantity() {
        return ApiResponse.success(inventoryApplicationService.getTotalInventoryQuantity());
    }

    /**
     * 方法功能描述：按分类统计库存
     *
     * @return 分类库存统计
     */
    @Operation(summary = "按分类统计库存", description = "按产品分类统计库存数量。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_BY_CATEGORY)
    @GetMapping("/statistics/by-category")
    public ApiResponse<List<Map<String, Object>>> getInventoryByCategory() {
        return ApiResponse.success(inventoryApplicationService.getInventoryByCategory());
    }

    /**
     * 方法功能描述：按位置统计库存
     *
     * @return 位置库存统计
     */
    @Operation(summary = "按位置统计库存", description = "按库位统计库存数量。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_BY_LOCATION)
    @GetMapping("/statistics/by-location")
    public ApiResponse<List<Map<String, Object>>> getInventoryByLocation() {
        return ApiResponse.success(inventoryApplicationService.getInventoryByLocation());
    }

    /**
     * 方法功能描述：获取产品库存统计
     *
     * @param productId 产品ID
     * @return 库存统计
     */
    @Operation(
            summary = "获取产品库存统计",
            description = "获取指定产品的库存统计信息，包括总库存、锁定库存、可用库存。成功返回200；产品不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.INVENTORY_PRODUCT_STATISTICS)
    @GetMapping("/statistics/product/{productId}")
    public ApiResponse<Map<String, Object>> getProductInventoryStatistics(
            @Parameter(description = "产品ID", required = true) @PathVariable("productId")
                    Long productId) {
        return ApiResponse.success(
                inventoryApplicationService.getProductInventoryStatistics(productId));
    }
}

package com.huicang.wise.api.controller;

import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.inventory.InventoryCreateRequest;
import com.huicang.wise.application.inventory.InventoryDTO;
import com.huicang.wise.application.inventory.InventoryUpdateRequest;
import com.huicang.wise.application.inventory.ProductCreateRequest;
import com.huicang.wise.application.inventory.ProductDTO;
import com.huicang.wise.application.inventory.ProductUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huicang.wise.application.inventory.InventoryReviewApplicationService;

import java.util.List;

/**
 * 类功能描述：库存管理控制层
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现产品与库存接口
 */
@Api(tags = "库存管理接口")
@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryApplicationService inventoryApplicationService;
    private final InventoryReviewApplicationService inventoryReviewApplicationService;

    public InventoryController(InventoryApplicationService inventoryApplicationService,
                               InventoryReviewApplicationService inventoryReviewApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.inventoryReviewApplicationService = inventoryReviewApplicationService;
    }

    /**
     * 方法功能描述：创建产品
     *
     * @param request 产品创建请求
     * @return 产品信息
     */
    @ApiOperation(
            value = "创建产品",
            notes = "创建产品信息。成功返回200；参数错误返回400；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.PRODUCT_CREATE)
    @PostMapping("/products")
    public ApiResponse<ProductDTO> createProduct(
            @ApiParam(value = "产品创建请求", required = true)
            @RequestBody ProductCreateRequest request) {
        return ApiResponse.success(inventoryApplicationService.createProduct(request));
    }

    /**
     * 方法功能描述：更新产品
     *
     * @param productId 产品主键ID
     * @param request   产品更新请求
     * @return 产品信息
     */
    @ApiOperation(
            value = "更新产品",
            notes = "更新产品信息。成功返回200；产品不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.PRODUCT_UPDATE)
    @PutMapping("/products/{productId}")
    public ApiResponse<ProductDTO> updateProduct(
            @ApiParam(value = "产品主键ID", required = true)
            @PathVariable("productId") Long productId,
            @ApiParam(value = "产品更新请求", required = true)
            @RequestBody ProductUpdateRequest request) {
        return ApiResponse.success(inventoryApplicationService.updateProduct(productId, request));
    }

    /**
     * 方法功能描述：查询产品详情
     *
     * @param productId 产品主键ID
     * @return 产品信息
     */
    @ApiOperation(
            value = "查询产品详情",
            notes = "查询产品详情信息。成功返回200；产品不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.PRODUCT_DETAIL)
    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDTO> getProduct(
            @ApiParam(value = "产品主键ID", required = true)
            @PathVariable("productId") Long productId) {
        return ApiResponse.success(inventoryApplicationService.getProduct(productId));
    }

    /**
     * 方法功能描述：创建库存明细
     *
     * @param request 库存创建请求
     * @return 库存明细信息
     */
    @ApiOperation(
            value = "创建库存明细",
            notes = "创建库存明细。成功返回200；参数错误返回400；产品不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.INVENTORY_CREATE)
    @PostMapping
    public ApiResponse<InventoryDTO> createInventory(
            @ApiParam(value = "库存创建请求", required = true)
            @RequestBody InventoryCreateRequest request) {
        return ApiResponse.success(inventoryApplicationService.createInventory(request));
    }

    /**
     * 方法功能描述：更新库存明细
     *
     * @param inventoryId 库存明细ID
     * @param request     库存更新请求
     * @return 库存明细信息
     */
    @ApiOperation(
            value = "更新库存明细",
            notes = "更新库存明细。成功返回200；库存不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.INVENTORY_UPDATE)
    @PutMapping("/{inventoryId}")
    public ApiResponse<InventoryDTO> updateInventory(
            @ApiParam(value = "库存明细ID", required = true)
            @PathVariable("inventoryId") Long inventoryId,
            @ApiParam(value = "库存更新请求", required = true)
            @RequestBody InventoryUpdateRequest request) {
        return ApiResponse.success(inventoryApplicationService.updateInventory(inventoryId, request));
    }

    /**
     * 方法功能描述：按产品查询库存列表
     *
     * @param productId 产品主键ID
     * @return 库存明细列表
     */
    @ApiOperation(
            value = "按产品查询库存",
            notes = "根据产品ID查询库存列表。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.INVENTORY_LIST)
    @GetMapping
    public ApiResponse<List<InventoryDTO>> listInventoryByProduct(
            @ApiParam(value = "产品主键ID", required = true)
            @RequestParam("productId") Long productId) {
        return ApiResponse.success(inventoryApplicationService.listInventoryByProduct(productId));
    }

    /**
     * 方法功能描述：搜索库存
     *
     * @param keyword 搜索关键字
     * @param type    搜索类型（PRODUCT/LOCATION）
     * @return 搜索结果
     */
    @ApiOperation(
            value = "搜索库存",
            notes = "搜索产品或按库位搜索库存。type=PRODUCT返回产品列表，type=LOCATION返回库存列表。"
    )
    @ApiPacketType(PacketType.INVENTORY_SEARCH)
    @GetMapping("/search")
    public ApiResponse<Object> search(
            @ApiParam(value = "搜索关键字", required = true)
            @RequestParam("keyword") String keyword,
            @ApiParam(value = "搜索类型(PRODUCT/LOCATION)", required = true)
            @RequestParam("type") String type) {
        if ("PRODUCT".equalsIgnoreCase(type)) {
            return ApiResponse.success(inventoryApplicationService.searchProducts(keyword));
        } else if ("LOCATION".equalsIgnoreCase(type)) {
            return ApiResponse.success(inventoryApplicationService.searchInventoryByLocation(keyword));
        }
        return ApiResponse.success(List.of());
    }

    /**
     * 方法功能描述：获取差异列表
     *
     * @param status 状态过滤(0-待处理, 1-已处理)
     * @return 差异列表
     */
    @ApiOperation(
            value = "获取差异列表",
            notes = "获取库存差异列表。支持按状态过滤。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.INVENTORY_DIFF_LIST)
    @GetMapping("/diffs")
    public ApiResponse<List<com.huicang.wise.application.inventory.InventoryDifferenceDTO>> listDifferences(
            @ApiParam(value = "状态(0-待处理, 1-已处理)", required = false)
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResponse.success(inventoryReviewApplicationService.listDifferences(status));
    }

    /**
     * 方法功能描述：复核差异
     *
     * @param diffId  差异ID
     * @param request 复核请求
     * @return 无
     */
    @ApiOperation(
            value = "复核差异",
            notes = "复核并处理库存差异。action=CORRECT修正库存，action=CONFIRM仅标记已处理。成功返回200；差异不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.INVENTORY_DIFF_REVIEW)
    @PostMapping("/diffs/{diffId}/review")
    public ApiResponse<Void> reviewDifference(
            @ApiParam(value = "差异ID", required = true)
            @PathVariable("diffId") Long diffId,
            @ApiParam(value = "复核请求", required = true)
            @RequestBody com.huicang.wise.application.inventory.ReviewDifferenceRequest request) {
        inventoryReviewApplicationService.reviewDifference(diffId, request);
        return ApiResponse.success(null);
    }
}


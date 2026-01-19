package com.huicang.wise.api.controller;

import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.inventory.InventoryCreateRequest;
import com.huicang.wise.application.inventory.InventoryDTO;
import com.huicang.wise.application.inventory.InventoryUpdateRequest;
import com.huicang.wise.application.inventory.ProductCreateRequest;
import com.huicang.wise.application.inventory.ProductDTO;
import com.huicang.wise.application.inventory.ProductUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
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

    public InventoryController(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
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
    @GetMapping
    public ApiResponse<List<InventoryDTO>> listInventoryByProduct(
            @ApiParam(value = "产品主键ID", required = true)
            @RequestParam("productId") Long productId) {
        return ApiResponse.success(inventoryApplicationService.listInventoryByProduct(productId));
    }
}


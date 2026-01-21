package com.huicang.wise.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.inventory.ProductCreateRequest;
import com.huicang.wise.application.inventory.ProductDTO;
import com.huicang.wise.application.inventory.ProductPageDTO;
import com.huicang.wise.common.api.ApiResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "库存服务")
@RestController
@RequestMapping("/product")
public class ProductController {

    private final InventoryApplicationService inventoryApplicationService;

    public ProductController(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    @ApiOperation(
            value = "创建产品",
            notes = "创建产品信息。成功返回200；参数错误返回400；服务器异常返回500。"
    )
    @PostMapping
    public ApiResponse<ProductDTO> createProduct(
            @ApiParam(value = "产品创建请求", required = true)
            @RequestBody ProductCreateRequest request) {
        return ApiResponse.success(inventoryApplicationService.createProduct(request));
    }

    @ApiOperation(
            value = "获取产品列表",
            notes = "获取产品分页列表。成功返回200；服务器异常返回500。"
    )
    @GetMapping
    public ApiResponse<ProductPageDTO> listProducts(
            @ApiParam(value = "页码", required = false)
            @RequestParam(value = "page", required = false) Integer page,
            @ApiParam(value = "每页数量", required = false)
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.success(inventoryApplicationService.listProducts(page, size));
    }

    @ApiOperation(
            value = "获取产品详情",
            notes = "获取产品详情。成功返回200；产品不存在返回404；服务器异常返回500。"
    )
    @GetMapping("/{product_id}")
    public ApiResponse<ProductDTO> getProduct(
            @ApiParam(value = "产品ID", required = true)
            @PathVariable("product_id") Long productId) {
        return ApiResponse.success(inventoryApplicationService.getProduct(productId));
    }
}

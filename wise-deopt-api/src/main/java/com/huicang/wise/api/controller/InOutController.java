package com.huicang.wise.api.controller;

import com.huicang.wise.application.inout.InOutApplicationService;
import com.huicang.wise.application.inout.StockOrderCreateRequest;
import com.huicang.wise.application.inout.StockOrderDTO;
import com.huicang.wise.application.inout.StockOrderPageDTO;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类功能描述：出入库管理控制层
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现出入库单接口
 */
@Api(tags = "出入库管理接口")
@RestController
@RequestMapping({"/api/stock-orders", "/stock-order"})
public class InOutController {

    private final InOutApplicationService inOutApplicationService;

    public InOutController(InOutApplicationService inOutApplicationService) {
        this.inOutApplicationService = inOutApplicationService;
    }

    /**
     * 方法功能描述：创建出入库单
     *
     * @param request 出入库单创建请求
     * @return 出入库单信息
     */
    @ApiOperation(
            value = "创建出入库单",
            notes = "创建出入库单。成功返回200；参数错误返回400；服务器异常返回500。"
    )
    @PostMapping
    public ApiResponse<StockOrderDTO> createStockOrder(
            @ApiParam(value = "出入库单创建请求", required = true)
            @RequestBody StockOrderCreateRequest request) {
        return ApiResponse.success(inOutApplicationService.createStockOrder(request));
    }

    @ApiOperation(
            value = "获取出入库单列表",
            notes = "获取出入库单分页列表。成功返回200；服务器异常返回500。"
    )
    @GetMapping
    public ApiResponse<StockOrderPageDTO> listStockOrders(
            @ApiParam(value = "页码", required = false)
            @RequestParam(value = "page", required = false) Integer page,
            @ApiParam(value = "每页数量", required = false)
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.success(inOutApplicationService.listStockOrders(page, size));
    }

    @ApiOperation(
            value = "提交出入库单",
            notes = "提交出入库单。成功返回200；出入库单不存在返回404；服务器异常返回500。"
    )
    @PostMapping("/{orderId}/submit")
    public ApiResponse<StockOrderDTO> submitStockOrder(
            @ApiParam(value = "出入库单ID", required = true)
            @PathVariable("orderId") Long orderId) {
        return ApiResponse.success(inOutApplicationService.submitStockOrder(orderId));
    }

    /**
     * 方法功能描述：查询出入库单详情
     *
     * @param orderId 出入库单ID
     * @return 出入库单信息
     */
    @ApiOperation(
            value = "查询出入库单详情",
            notes = "查询出入库单详情。成功返回200；出入库单不存在返回404；服务器异常返回500。"
    )
    @GetMapping("/{orderId}")
    public ApiResponse<StockOrderDTO> getStockOrder(
            @ApiParam(value = "出入库单ID", required = true)
            @PathVariable("orderId") Long orderId) {
        return ApiResponse.success(inOutApplicationService.getStockOrder(orderId));
    }
}

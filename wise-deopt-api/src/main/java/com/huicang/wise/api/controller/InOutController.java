package com.huicang.wise.api.controller;

import com.huicang.wise.application.inout.InOutApplicationService;
import com.huicang.wise.application.inout.StockOrderAuditRequest;
import com.huicang.wise.application.inout.StockOrderCreateRequest;
import com.huicang.wise.application.inout.StockOrderDTO;
import com.huicang.wise.application.inout.StockOrderItemDTO;
import com.huicang.wise.application.inout.StockOrderPageDTO;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "出入库管理接口")
@RestController
@RequestMapping("/api/stock-orders")
public class InOutController {

    private final InOutApplicationService inOutApplicationService;

    public InOutController(InOutApplicationService inOutApplicationService) {
        this.inOutApplicationService = inOutApplicationService;
    }

    @Operation(summary = "创建出入库单", description = "创建出入库单。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.STOCK_ORDER_CREATE)
    @PostMapping
    public ApiResponse<StockOrderDTO> createStockOrder(
            @Parameter(description = "出入库单创建请求", required = true) @Valid @RequestBody
                    StockOrderCreateRequest request) {
        return ApiResponse.success(inOutApplicationService.createStockOrder(request));
    }

    @Operation(summary = "获取出入库单列表", description = "获取出入库单分页列表。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.STOCK_ORDER_LIST)
    @GetMapping
    public ApiResponse<StockOrderPageDTO> listStockOrders(
            @Parameter(description = "页码", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页数量", required = false)
                    @RequestParam(value = "size", required = false)
                    Integer size) {
        return ApiResponse.success(inOutApplicationService.listStockOrders(page, size));
    }

    @Operation(summary = "提交出入库单", description = "提交出入库单。成功返回200；出入库单不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.STOCK_ORDER_SUBMIT)
    @PostMapping("/{orderId}/submit")
    public ApiResponse<StockOrderDTO> submitStockOrder(
            @Parameter(description = "出入库单ID", required = true) @PathVariable("orderId")
                    Long orderId) {
        return ApiResponse.success(inOutApplicationService.submitStockOrder(orderId));
    }

    @Operation(summary = "审核出入库单", description = "审核出入库单。成功返回200；出入库单不存在返回404；服务器异常返回500。")
    @PostMapping("/{orderId}/audit")
    public ApiResponse<StockOrderDTO> auditStockOrder(
            @Parameter(description = "出入库单ID", required = true) @PathVariable("orderId")
                    Long orderId,
            @Valid @RequestBody StockOrderAuditRequest request) {
        return ApiResponse.success(
                inOutApplicationService.auditStockOrder(
                        orderId, request.getApproved(), request.getReason()));
    }

    @Operation(summary = "撤回出入库单", description = "撤回出入库单。成功返回200；出入库单不存在返回404；服务器异常返回500。")
    @PostMapping("/{orderId}/withdraw")
    public ApiResponse<StockOrderDTO> withdrawStockOrder(
            @Parameter(description = "出入库单ID", required = true) @PathVariable("orderId")
                    Long orderId) {
        return ApiResponse.success(inOutApplicationService.withdrawStockOrder(orderId));
    }

    @Operation(summary = "添加出入库明细", description = "为出入库单添加明细。成功返回200；出入库单不存在返回404；服务器异常返回500。")
    @PostMapping("/{orderId}/items")
    public ApiResponse<StockOrderDTO> addItem(
            @Parameter(description = "出入库单ID", required = true) @PathVariable("orderId")
                    Long orderId,
            @Parameter(description = "出入库明细", required = true) @Valid @RequestBody
                    StockOrderItemDTO item) {
        return ApiResponse.success(inOutApplicationService.addItem(orderId, item));
    }

    @Operation(summary = "删除出入库明细", description = "删除出入库明细。成功返回200；明细不存在返回404；服务器异常返回500。")
    @PostMapping("/{orderId}/items/{tagId}/remove")
    public ApiResponse<Void> removeItem(
            @Parameter(description = "出入库单ID", required = true) @PathVariable("orderId")
                    Long orderId,
            @Parameter(description = "标签ID", required = true) @PathVariable("tagId") Long tagId) {
        inOutApplicationService.removeItem(orderId, tagId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "修改出入库单", description = "修改出入库单。成功返回200；出入库单不存在返回404；服务器异常返回500。")
    @PostMapping("/{orderId}/update")
    public ApiResponse<StockOrderDTO> updateStockOrder(
            @Parameter(description = "出入库单ID", required = true) @PathVariable("orderId")
                    Long orderId,
            @Valid @RequestBody StockOrderCreateRequest request) {
        return ApiResponse.success(inOutApplicationService.updateStockOrder(orderId, request));
    }

    @Operation(summary = "查询出入库单详情", description = "查询出入库单详情。成功返回200；出入库单不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.STOCK_ORDER_DETAIL)
    @GetMapping("/{orderId}")
    public ApiResponse<StockOrderDTO> getStockOrder(
            @Parameter(description = "出入库单ID", required = true) @PathVariable("orderId")
                    Long orderId) {
        return ApiResponse.success(inOutApplicationService.getStockOrder(orderId));
    }
}

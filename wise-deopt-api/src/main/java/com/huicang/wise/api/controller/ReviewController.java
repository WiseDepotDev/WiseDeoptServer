package com.huicang.wise.api.controller;

import com.huicang.wise.application.inventory.InventoryDifferenceDTO;
import com.huicang.wise.application.inventory.InventoryReviewApplicationService;
import com.huicang.wise.application.inventory.ReviewDifferenceRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 类功能描述：库存差异复核控制层
 *
 * @author xingchentye
 * @date 2026-01-25
 */
@Api(tags = "库存差异复核接口")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final InventoryReviewApplicationService reviewApplicationService;

    public ReviewController(InventoryReviewApplicationService reviewApplicationService) {
        this.reviewApplicationService = reviewApplicationService;
    }

    @ApiOperation(
            value = "获取差异列表",
            notes = "获取库存差异列表。支持按状态过滤。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.INVENTORY_DIFF_LIST)
    @GetMapping
    public ApiResponse<List<InventoryDifferenceDTO>> listDifferences(
            @ApiParam(value = "状态(0:待处理, 1:已处理)", required = false)
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResponse.success(reviewApplicationService.listDifferences(status));
    }

    @ApiOperation(
            value = "复核差异",
            notes = "复核/处理差异。成功返回200；差异不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.INVENTORY_DIFF_REVIEW)
    @PutMapping("/{diffId}")
    public ApiResponse<Void> reviewDifference(
            @ApiParam(value = "差异ID", required = true)
            @PathVariable("diffId") Long diffId,
            @ApiParam(value = "复核请求", required = true)
            @RequestBody ReviewDifferenceRequest request) {
        reviewApplicationService.reviewDifference(diffId, request);
        return ApiResponse.success();
    }

    @ApiOperation(
            value = "创建差异(测试用)",
            notes = "手动创建差异记录。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.INVENTORY_DIFF_LIST) // Reuse list type or create new if needed
    @PostMapping
    public ApiResponse<InventoryDifferenceDTO> createDifference(
            @RequestBody InventoryDifferenceDTO dto) {
        return ApiResponse.success(reviewApplicationService.createDifference(dto));
    }
}

package com.huicang.wise.api.controller;

import com.huicang.wise.application.warehouse.WarehouseApplicationService;
import com.huicang.wise.application.warehouse.WarehouseCreateRequest;
import com.huicang.wise.application.warehouse.WarehouseDTO;
import com.huicang.wise.application.warehouse.WarehouseUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 仓库管理接口
 *
 * @author WiseDepot
 * @version 0.0.1
 * @since 2026-03-14
 */
@Tag(name = "仓库管理", description = "仓库管理相关接口")
@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseApplicationService warehouseApplicationService;

    public WarehouseController(WarehouseApplicationService warehouseApplicationService) {
        this.warehouseApplicationService = warehouseApplicationService;
    }

    @Operation(summary = "查询仓库列表", description = "查询仓库列表，支持按关键字搜索。")
    @GetMapping
    public ApiResponse<List<WarehouseDTO>> listWarehouses(
            @Parameter(description = "搜索关键字", required = false)
                    @RequestParam(value = "keyword", required = false)
                    String keyword) {
        return ApiResponse.success(warehouseApplicationService.listWarehouses(keyword));
    }

    @Operation(summary = "创建仓库", description = "创建新仓库")
    @PostMapping
    public ApiResponse<WarehouseDTO> createWarehouse(
            @RequestBody @Valid WarehouseCreateRequest request) {
        return ApiResponse.success(warehouseApplicationService.createWarehouse(request));
    }

    @Operation(summary = "更新仓库", description = "更新仓库信息")
    @PutMapping("/{id}")
    public ApiResponse<WarehouseDTO> updateWarehouse(
            @Parameter(description = "仓库ID", required = true) @PathVariable Long id,
            @Valid @RequestBody WarehouseUpdateRequest request) {
        return ApiResponse.success(warehouseApplicationService.updateWarehouse(id, request));
    }

    @Operation(summary = "删除仓库", description = "删除仓库")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWarehouse(
            @Parameter(description = "仓库ID", required = true) @PathVariable Long id) {
        warehouseApplicationService.deleteWarehouse(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取仓库详情", description = "获取仓库详情")
    @GetMapping("/{id}")
    public ApiResponse<WarehouseDTO> getWarehouse(
            @Parameter(description = "仓库ID", required = true) @PathVariable Long id) {
        return ApiResponse.success(warehouseApplicationService.getWarehouse(id));
    }
}

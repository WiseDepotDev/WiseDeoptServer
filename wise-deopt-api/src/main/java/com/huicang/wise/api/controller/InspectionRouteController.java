package com.huicang.wise.api.controller;

import com.huicang.wise.application.inspection.CreateInspectionRouteRequest;
import com.huicang.wise.application.inspection.InspectionApplicationService;
import com.huicang.wise.application.inspection.InspectionRouteDTO;
import com.huicang.wise.application.inspection.UpdateInspectionRouteRequest;
import com.huicang.wise.common.api.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 巡检路线控制器
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@RestController
@RequestMapping("/inspection/route")
@Api(tags = "巡检路线控制器")
public class InspectionRouteController {

    private final InspectionApplicationService inspectionApplicationService;

    public InspectionRouteController(InspectionApplicationService inspectionApplicationService) {
        this.inspectionApplicationService = inspectionApplicationService;
    }

    @PostMapping
    @ApiOperation("创建巡检路线")
    public ApiResponse<InspectionRouteDTO> createRoute(@RequestBody CreateInspectionRouteRequest request) {
        return ApiResponse.success(inspectionApplicationService.createRoute(request));
    }

    @PutMapping
    @ApiOperation("更新巡检路线")
    public ApiResponse<InspectionRouteDTO> updateRoute(@RequestBody UpdateInspectionRouteRequest request) {
        return ApiResponse.success(inspectionApplicationService.updateRoute(request));
    }

    @DeleteMapping("/{routeId}")
    @ApiOperation("删除巡检路线")
    public ApiResponse<Void> deleteRoute(@PathVariable Long routeId) {
        inspectionApplicationService.deleteRoute(routeId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{routeId}")
    @ApiOperation("根据ID获取巡检路线")
    public ApiResponse<InspectionRouteDTO> getRoute(@PathVariable Long routeId) {
        return ApiResponse.success(inspectionApplicationService.getRoute(routeId));
    }

    @GetMapping
    @ApiOperation("获取所有巡检路线")
    public ApiResponse<List<InspectionRouteDTO>> listRoutes() {
        return ApiResponse.success(inspectionApplicationService.listRoutes());
    }
}

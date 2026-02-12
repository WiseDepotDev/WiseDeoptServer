package com.huicang.wise.api.controller;

import com.huicang.wise.application.dashboard.DashboardApplicationService;
import com.huicang.wise.application.dashboard.DashboardSummaryDTO;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类功能描述：看板控制器
 *
 * @author xingchentye
 * @since 2026-02-02
 */
@Tag(name = "Dashboard", description = "首页看板")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardApplicationService dashboardApplicationService;

    public DashboardController(DashboardApplicationService dashboardApplicationService) {
        this.dashboardApplicationService = dashboardApplicationService;
    }

    @ApiPacketType(PacketType.DASHBOARD_SUMMARY)
    @Operation(summary = "获取首页看板汇总信息", description = "获取库存总量、今日报警数、巡检状态、设备在线数")
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDTO> getSummary() {
        return ApiResponse.success(dashboardApplicationService.getSummary());
    }
}

package com.huicang.wise.api.controller;

import com.huicang.wise.application.dashboard.DashboardApplicationService;
import com.huicang.wise.application.dashboard.DashboardSummaryDTO;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "首页看板接口")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardApplicationService dashboardApplicationService;

    public DashboardController(DashboardApplicationService dashboardApplicationService) {
        this.dashboardApplicationService = dashboardApplicationService;
    }

    @ApiOperation(
            value = "获取首页KPI汇总",
            notes = "获取库存总量、今日报警、巡检进度、设备在线数。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.DASHBOARD_SUMMARY)
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDTO> getSummary() {
        return ApiResponse.success(dashboardApplicationService.getSummary());
    }
}

package com.huicang.wise.api.controller;

import com.huicang.wise.application.report.InventoryLedgerDTO;
import com.huicang.wise.application.report.ReconciliationReportDTO;
import com.huicang.wise.application.report.ReportApplicationService;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报表管理控制器
 *
 * @author xingchentye
 * @date 2026-01-26
 */
@Api(tags = "报表管理接口")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportApplicationService reportApplicationService;

    public ReportController(ReportApplicationService reportApplicationService) {
        this.reportApplicationService = reportApplicationService;
    }

    @ApiOperation(
            value = "获取库存台账",
            notes = "获取指定产品的库存变动台账。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.REPORT_LEDGER)
    @GetMapping("/ledger")
    public ApiResponse<List<InventoryLedgerDTO>> getInventoryLedger(
            @ApiParam(value = "产品ID", required = true)
            @RequestParam("productId") Long productId) {
        return ApiResponse.success(reportApplicationService.getInventoryLedger(productId));
    }

    @ApiOperation(
            value = "获取对账报表（差异分析）",
            notes = "获取库存差异对账报表。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.REPORT_RECONCILIATION)
    @GetMapping("/reconciliation")
    public ApiResponse<List<ReconciliationReportDTO>> getReconciliationReport(
            @ApiParam(value = "状态过滤：0-待处理 1-已处理", required = false)
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResponse.success(reportApplicationService.getReconciliationReport(status));
    }
}

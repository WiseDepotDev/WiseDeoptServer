package com.huicang.wise.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huicang.wise.application.report.ReportApplicationService;
import com.huicang.wise.application.report.ReportTaskCreateRequest;
import com.huicang.wise.application.report.ReportTaskDTO;
import com.huicang.wise.application.report.ReportExportRecordDTO;
import com.huicang.wise.common.api.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * 类功能描述：报表管理控制层
 *
 * @author WiseDepot
 * @version 0.1.18
 * @since 2026-02-27
 */
@Tag(name = "报表管理接口")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportApplicationService reportApplicationService;

    public ReportController(ReportApplicationService reportApplicationService) {
        this.reportApplicationService = reportApplicationService;
    }

    /**
     * 方法功能描述：创建报表任务
     *
     * @param request 报表任务创建请求
     * @return 报表任务
     */
    @Operation(summary = "创建报表任务", description = "创建报表生成任务。成功返回200；参数错误返回400；服务器异常返回500。")
    @PostMapping
    public ApiResponse<ReportTaskDTO> createReportTask(
            @Parameter(description = "报表任务创建请求", required = true)
            @RequestBody ReportTaskCreateRequest request) {
        return ApiResponse.success(reportApplicationService.createReportTask(request));
    }

    /**
     * 方法功能描述：执行报表任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @Operation(summary = "执行报表任务", description = "异步执行报表生成任务。成功返回200；任务不存在返回404；服务器异常返回500。")
    @PostMapping("/{taskId}/execute")
    public ApiResponse<Void> executeReportTask(
            @Parameter(description = "任务ID", required = true)
            @PathVariable("taskId") Long taskId) {
        reportApplicationService.executeReportTask(taskId);
        return ApiResponse.success();
    }

    /**
     * 方法功能描述：查询报表任务列表
     *
     * @param reportType 报表类型
     * @param status 任务状态
     * @param createBy 创建者ID
     * @return 报表任务列表
     */
    @Operation(summary = "查询报表任务列表", description = "查询报表任务列表。成功返回200；服务器异常返回500。")
    @GetMapping
    public ApiResponse<List<ReportTaskDTO>> listReportTasks(
            @Parameter(description = "任务状态", required = false)
            @RequestParam(value = "status", required = false) Short status) {
        return ApiResponse.success(reportApplicationService.listReportTasks(status));
    }

    /**
     * 方法功能描述：查询报表任务详情
     *
     * @param taskId 任务ID
     * @return 报表任务
     */
    @Operation(summary = "查询报表任务详情", description = "查询报表任务详情。成功返回200；任务不存在返回404；服务器异常返回500。")
    @GetMapping("/{taskId}")
    public ApiResponse<ReportTaskDTO> getReportTask(
            @Parameter(description = "任务ID", required = true)
            @PathVariable("taskId") Long taskId) {
        return ApiResponse.success(reportApplicationService.getReportTask(taskId));
    }

    /**
     * 方法功能描述：查询导出记录列表
     *
     * @param taskId 任务ID
     * @return 导出记录列表
     */
    @Operation(summary = "查询导出记录列表", description = "查询报表导出记录列表。成功返回200；服务器异常返回500。")
    @GetMapping("/{taskId}/exports")
    public ApiResponse<List<ReportExportRecordDTO>> listExportRecords(
            @Parameter(description = "任务ID", required = true)
            @PathVariable("taskId") Long taskId) {
        return ApiResponse.success(reportApplicationService.listExportRecords(taskId));
    }
}

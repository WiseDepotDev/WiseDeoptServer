package com.huicang.wise.api.controller;

import com.huicang.wise.application.inspection.InspectionApplicationService;
import com.huicang.wise.application.inspection.InspectionDifferenceVO;
import com.huicang.wise.application.inspection.InspectionPlanCreateRequest;
import com.huicang.wise.application.inspection.InspectionPlanDTO;
import com.huicang.wise.application.inspection.InspectionPlanUpdateRequest;
import com.huicang.wise.application.inspection.InspectionResultDTO;
import com.huicang.wise.application.inspection.InspectionTaskCreateRequest;
import com.huicang.wise.application.inspection.InspectionTaskDTO;
import com.huicang.wise.application.inspection.InspectionTaskPageDTO;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "巡检管理", description = "巡检管理相关接口")
@RestController
@RequestMapping("/api/inspection")
public class InspectionController {

    @Autowired private InspectionApplicationService inspectionApplicationService;

    @Operation(summary = "获取巡检任务差异详情")
    @GetMapping("/task/{taskId}/diff")
    public ApiResponse<List<InspectionDifferenceVO>> getInspectionDifferences(
            @Parameter(description = "任务ID") @PathVariable("taskId") Long taskId) {
        List<InspectionDifferenceVO> diffs =
                inspectionApplicationService.getInspectionDifferences(taskId);
        return ApiResponse.success(diffs);
    }

    @Operation(summary = "创建巡检计划")
    @PostMapping("/plan")
    public ApiResponse<InspectionPlanDTO> createPlan(
            @Valid @RequestBody InspectionPlanCreateRequest request) {
        InspectionPlanDTO plan = inspectionApplicationService.createPlan(request);
        return ApiResponse.success(plan);
    }

    @Operation(summary = "更新巡检计划")
    @PutMapping("/plan/{planId}")
    public ApiResponse<InspectionPlanDTO> updatePlan(
            @Parameter(description = "计划ID") @PathVariable("planId") Long planId,
            @Valid @RequestBody InspectionPlanUpdateRequest request) {
        InspectionPlanDTO plan = inspectionApplicationService.updatePlan(planId, request);
        return ApiResponse.success(plan);
    }

    @Operation(summary = "删除巡检计划")
    @DeleteMapping("/plan/{planId}")
    public ApiResponse<Void> deletePlan(
            @Parameter(description = "计划ID") @PathVariable("planId") Long planId) {
        inspectionApplicationService.deletePlan(planId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取巡检计划详情")
    @GetMapping("/plan/{planId}")
    public ApiResponse<InspectionPlanDTO> getPlan(
            @Parameter(description = "计划ID") @PathVariable("planId") Long planId) {
        InspectionPlanDTO plan = inspectionApplicationService.getPlan(planId);
        return ApiResponse.success(plan);
    }

    @Operation(summary = "查询巡检计划列表")
    @GetMapping("/plan")
    public ApiResponse<List<InspectionPlanDTO>> listPlans(
            @Parameter(description = "计划类型") @RequestParam(value = "planType", required = false)
                    String planType,
            @Parameter(description = "是否启用") @RequestParam(value = "enabled", required = false)
                    Boolean enabled,
            @Parameter(description = "仓库ID") @RequestParam(value = "warehouseId", required = false)
                    Long warehouseId) {
        List<InspectionPlanDTO> plans =
                inspectionApplicationService.listPlans(planType, enabled, warehouseId);
        return ApiResponse.success(plans);
    }

    @Operation(summary = "创建巡检任务")
    @PostMapping("/task")
    public ApiResponse<InspectionTaskDTO> createTask(
            @Valid @RequestBody InspectionTaskCreateRequest request) {
        InspectionTaskDTO task = inspectionApplicationService.createTask(request);
        return ApiResponse.success(task);
    }

    @Operation(summary = "获取巡检任务详情")
    @GetMapping("/task/{taskId}")
    public ApiResponse<InspectionTaskDTO> getTask(
            @Parameter(description = "任务ID") @PathVariable("taskId") Long taskId) {
        InspectionTaskDTO task = inspectionApplicationService.getTask(taskId);
        return ApiResponse.success(task);
    }

    @Operation(summary = "查询巡检任务列表")
    @GetMapping("/task")
    public ApiResponse<List<InspectionTaskDTO>> listTasks(
            @Parameter(description = "计划ID") @RequestParam(value = "planId", required = false)
                    Long planId,
            @Parameter(description = "任务类型") @RequestParam(value = "taskType", required = false)
                    String taskType,
            @Parameter(description = "状态") @RequestParam(value = "status", required = false)
                    String status,
            @Parameter(description = "仓库ID") @RequestParam(value = "warehouseId", required = false)
                    Long warehouseId,
            @Parameter(description = "设备ID") @RequestParam(value = "deviceId", required = false)
                    Long deviceId) {
        List<InspectionTaskDTO> tasks =
                inspectionApplicationService.listTasks(
                        planId, taskType, status, warehouseId, deviceId);
        return ApiResponse.success(tasks);
    }

    @Operation(summary = "查询巡检任务列表（分页）")
    @GetMapping("/task/page")
    public ApiResponse<InspectionTaskPageDTO> listTasksPage(
            @Parameter(description = "计划ID") @RequestParam(value = "planId", required = false)
                    Long planId,
            @Parameter(description = "任务类型") @RequestParam(value = "taskType", required = false)
                    String taskType,
            @Parameter(description = "状态") @RequestParam(value = "status", required = false)
                    String status,
            @Parameter(description = "仓库ID") @RequestParam(value = "warehouseId", required = false)
                    Long warehouseId,
            @Parameter(description = "设备ID") @RequestParam(value = "deviceId", required = false)
                    Long deviceId,
            @Parameter(description = "页码（从1开始）")
                    @RequestParam(value = "page", required = false, defaultValue = "1")
                    Integer page,
            @Parameter(description = "每页记录数")
                    @RequestParam(value = "pageSize", required = false, defaultValue = "10")
                    Integer pageSize) {
        InspectionTaskPageDTO pageDTO =
                inspectionApplicationService.listTasks(
                        planId, taskType, status, warehouseId, deviceId, page, pageSize);
        return ApiResponse.success(pageDTO);
    }

    @Operation(summary = "更新巡检任务状态")
    @PutMapping("/task/{taskId}/status")
    public ApiResponse<Void> updateTaskStatus(
            @Parameter(description = "任务ID") @PathVariable("taskId") Long taskId,
            @Parameter(description = "状态") @RequestParam("status") String status) {
        inspectionApplicationService.updateTaskStatus(taskId, status);

        // Broadcast status update
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("taskId", taskId);
        data.put("status", status);
        // We might want to send the full task or at least progress 100 if completed
        if ("COMPLETED".equals(status)) {
            data.put("progress", 100);
        }
        com.huicang.wise.api.websocket.InspectionProgressWebSocket.broadcastProgress(
                taskId.toString(), data);

        return ApiResponse.success();
    }

    @Operation(summary = "上报巡检进度")
    @PutMapping("/task/{taskId}/progress")
    public ApiResponse<Void> updateTaskProgress(
            @Parameter(description = "任务ID") @PathVariable("taskId") Long taskId,
            @Parameter(description = "进度(0-100)") @RequestParam("progress") Integer progress,
            @Parameter(description = "已扫描数量")
                    @RequestParam(value = "scannedCount", required = false)
                    Integer scannedCount) {

        inspectionApplicationService.updateTaskProgress(taskId, progress, scannedCount);

        // Broadcast to WebSocket
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("taskId", taskId);
        data.put("progress", progress);
        if (scannedCount != null) {
            data.put("scannedCount", scannedCount);
        }
        com.huicang.wise.api.websocket.InspectionProgressWebSocket.broadcastProgress(
                taskId.toString(), data);
        return ApiResponse.success();
    }

    @Operation(summary = "创建巡检结果")
    @PostMapping("/result")
    public ApiResponse<InspectionResultDTO> createResult(
            @Parameter(description = "任务ID") @RequestParam("taskId") Long taskId,
            @Parameter(description = "总数量") @RequestParam("totalItems") Integer totalItems,
            @Parameter(description = "正常数量") @RequestParam("normalItems") Integer normalItems,
            @Parameter(description = "异常数量") @RequestParam("abnormalItems") Integer abnormalItems,
            @Parameter(description = "盘亏数量") @RequestParam("missingItems") Integer missingItems,
            @Parameter(description = "盘盈数量") @RequestParam("extraItems") Integer extraItems) {
        InspectionResultDTO result =
                inspectionApplicationService.createResult(
                        taskId, totalItems, normalItems, abnormalItems, missingItems, extraItems);

        // Broadcast completion
        java.util.Map<String, Object> wsData = new java.util.HashMap<>();
        wsData.put("taskId", taskId);
        wsData.put("progress", 100);
        wsData.put("status", "COMPLETED");
        wsData.put("result", result);
        com.huicang.wise.api.websocket.InspectionProgressWebSocket.broadcastProgress(
                taskId.toString(), wsData);

        return ApiResponse.success(result);
    }

    @Operation(summary = "上报巡检结果与明细")
    @ApiPacketType(PacketType.RFID_DATA_UPLOAD)
    @PostMapping("/report")
    public ApiResponse<InspectionResultDTO> reportResult(
            @Valid @RequestBody
                    com.huicang.wise.application.inspection.InspectionReportRequest request) {
        InspectionResultDTO result = inspectionApplicationService.reportResult(request);

        // Progress will be broadcasted via event mechanism
        // No need to manually broadcast here as it's handled in the service layer

        return ApiResponse.success(result);
    }

    @Operation(summary = "获取巡检结果详情")
    @GetMapping("/result/{resultId}")
    public ApiResponse<InspectionResultDTO> getResult(
            @Parameter(description = "结果ID") @PathVariable("resultId") Long resultId) {
        InspectionResultDTO result = inspectionApplicationService.getResult(resultId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询巡检结果列表")
    @GetMapping("/result")
    public ApiResponse<List<InspectionResultDTO>> listResults(
            @Parameter(description = "任务ID") @RequestParam(value = "taskId", required = false)
                    Long taskId,
            @Parameter(description = "仓库ID") @RequestParam(value = "warehouseId", required = false)
                    Long warehouseId,
            @Parameter(description = "状态") @RequestParam(value = "status", required = false)
                    String status) {
        List<InspectionResultDTO> results =
                inspectionApplicationService.listResults(taskId, warehouseId, status);
        return ApiResponse.success(results);
    }

    @Operation(summary = "差异确认与处理")
    @PostMapping("/result/{resultId}/confirm")
    public ApiResponse<Void> confirmResult(
            @Parameter(description = "结果ID") @PathVariable("resultId") Long resultId,
            @Valid @RequestBody java.util.Map<String, Object> request) {
        // Here we can process the differences (e.g. create adjustments, update inventory)
        // For demonstration, we just return success
        return ApiResponse.success();
    }

    @Operation(summary = "一键导出PDF报告")
    @GetMapping("/result/{resultId}/export/pdf")
    public void exportPdf(
            @Parameter(description = "结果ID") @PathVariable("resultId") Long resultId,
            jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"inspection_report_" + resultId + ".pdf\"");
        response.getWriter().write("%PDF-1.4\n%...\n(Mock PDF Content for " + resultId + ")");
        response.getWriter().flush();
    }

    @Operation(summary = "手动补录巡检明细")
    @PostMapping("/task/{taskId}/manual-record")
    public ApiResponse<Void> manualRecord(
            @Parameter(description = "任务ID") @PathVariable("taskId") Long taskId,
            @Valid @RequestBody
                    com.huicang.wise.application.inspection.ManualRecordRequest request) {
        request.setTaskId(taskId);
        inspectionApplicationService.manualRecord(request);
        return ApiResponse.success();
    }
}

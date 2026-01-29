package com.huicang.wise.api.controller;

import com.huicang.wise.application.inspection.*;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.api.PageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 巡检控制器
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@RestController
@RequestMapping("/inspection")
@Api(tags = "巡检控制器")
public class InspectionController {

    private final InspectionApplicationService inspectionApplicationService;

    public InspectionController(InspectionApplicationService inspectionApplicationService) {
        this.inspectionApplicationService = inspectionApplicationService;
    }

    // --- 计划管理 ---

    @PostMapping("/plan")
    @ApiOperation("创建巡检计划")
    public ApiResponse<InspectionPlanVO> createPlan(
            @RequestBody CreateInspectionPlanDTO request) {
        return ApiResponse.success(inspectionApplicationService.createPlan(request));
    }

    @PutMapping("/plan")
    @ApiOperation("更新巡检计划")
    public ApiResponse<InspectionPlanVO> updatePlan(
            @RequestBody UpdateInspectionPlanDTO request) {
        return ApiResponse.success(inspectionApplicationService.updatePlan(request));
    }

    @DeleteMapping("/plan/{planId}")
    @ApiOperation("删除巡检计划")
    public ApiResponse<Void> deletePlan(@PathVariable Long planId) {
        inspectionApplicationService.deletePlan(planId);
        return ApiResponse.success(null);
    }

    @GetMapping("/plan/{planId}")
    @ApiOperation("根据ID获取巡检计划")
    public ApiResponse<InspectionPlanVO> getPlan(@PathVariable Long planId) {
        return ApiResponse.success(inspectionApplicationService.getPlan(planId));
    }

    @GetMapping("/plans")
    @ApiOperation("获取所有巡检计划")
    public ApiResponse<List<InspectionPlanVO>> listPlans() {
        return ApiResponse.success(inspectionApplicationService.listPlans());
    }

    @PutMapping("/plan/{planId}/status")
    @ApiOperation("更新巡检计划状态")
    public ApiResponse<Void> updatePlanStatus(
            @PathVariable Long planId,
            @RequestParam Integer status) {
        inspectionApplicationService.updatePlanStatus(planId, status);
        return ApiResponse.success(null);
    }

    // --- 任务管理 ---

    @GetMapping("/tasks")
    @ApiOperation("分页查询巡检任务")
    public ApiResponse<PageVO<InspectionTaskVO>> listTasks(
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Integer taskType,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<InspectionTaskVO> list = inspectionApplicationService.listTasks(planId, taskType, deviceId, status);
        // 简单内存分页
        int total = list.size();
        int fromIndex = (page - 1) * size;
        if (fromIndex >= total) {
            return ApiResponse.success(new PageVO<>(page, size, total, Collections.<InspectionTaskVO>emptyList()));
        }
        int toIndex = Math.min(fromIndex + size, total);
        return ApiResponse.success(new PageVO<>(page, size, total, list.subList(fromIndex, toIndex)));
    }

    @GetMapping("/task/device/{deviceId}")
    @ApiOperation("巡检设备查询自己的巡检任务（待执行）")
    public ApiResponse<List<InspectionTaskVO>> listPendingTasksForDevice(@PathVariable Long deviceId) {
        return ApiResponse.success(inspectionApplicationService.listPendingTasksForDevice(deviceId));
    }

    @GetMapping("/task/device/{deviceId}/history")
    @ApiOperation("查询设备历史巡检任务")
    public ApiResponse<PageVO<InspectionTaskVO>> listHistoryTasksForDevice(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<InspectionTaskVO> list = inspectionApplicationService.listHistoryTasksForDevice(deviceId);
        int total = list.size();
        int fromIndex = (page - 1) * size;
        if (fromIndex >= total) {
            return ApiResponse.success(new PageVO<>(page, size, total, Collections.<InspectionTaskVO>emptyList()));
        }
        int toIndex = Math.min(fromIndex + size, total);
        return ApiResponse.success(new PageVO<>(page, size, total, list.subList(fromIndex, toIndex)));
    }

    @PostMapping("/task/manual")
    @ApiOperation("创建手动巡检任务")
    public ApiResponse<InspectionTaskVO> createManualTask(@RequestParam Long deviceId) {
        return ApiResponse.success(inspectionApplicationService.createManualTask(deviceId));
    }

    @PostMapping("/task/start/{taskId}")
    @ApiOperation("巡检设备开始执行任务")
    public ApiResponse<InspectionTaskVO> startTask(@PathVariable Long taskId) {
        return ApiResponse.success(inspectionApplicationService.startTask(taskId));
    }

    @PostMapping("/task/complete/{taskId}")
    @ApiOperation("巡检设备完成任务")
    public ApiResponse<InspectionTaskVO> completeTask(@PathVariable Long taskId) {
        return ApiResponse.success(inspectionApplicationService.completeTask(taskId));
    }

    // --- 结果与明细 ---

    @PostMapping("/detail/report")
    @ApiOperation("上报巡检明细")
    public ApiResponse<Void> reportDetails(@RequestBody List<InspectionDetailDTO> details) {
        inspectionApplicationService.reportDetails(details);
        return ApiResponse.success(null);
    }

    @PostMapping("/result/process/{taskId}")
    @ApiOperation("处理巡检结果")
    public ApiResponse<Void> processResult(@PathVariable Long taskId) {
        inspectionApplicationService.processResult(taskId);
        return ApiResponse.success(null);
    }

    @GetMapping("/result/{taskId}")
    @ApiOperation("获取巡检结果汇总")
    public ApiResponse<InspectionResultSummaryVO> getResultSummary(@PathVariable Long taskId) {
        return ApiResponse.success(inspectionApplicationService.getResultSummary(taskId));
    }

    @GetMapping("/statistics")
    @ApiOperation("获取巡检统计信息（看板KPI）")
    public ApiResponse<InspectionStatisticsVO> getStatistics() {
        return ApiResponse.success(inspectionApplicationService.getStatistics());
    }

    @GetMapping("/result/{taskId}/missing")
    @ApiOperation("获取巡检缺失项明细")
    public ApiResponse<List<InspectionMissingItemVO>> getMissingItems(@PathVariable Long taskId) {
        return ApiResponse.success(inspectionApplicationService.getMissingItems(taskId));
    }
}

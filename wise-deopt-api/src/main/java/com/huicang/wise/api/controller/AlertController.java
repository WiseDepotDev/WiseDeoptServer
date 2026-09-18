package com.huicang.wise.api.controller;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.alert.AlertEventPageDTO;
import com.huicang.wise.application.alert.AlertHandleLogPageDTO;
import com.huicang.wise.application.alert.UpdateAlertStatusRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类功能描述：告警管理控制层
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现告警接口
 */
@Tag(name = "告警管理接口")
@RestController
@RequestMapping({"/api/alerts", "/alert"})
public class AlertController {

    private final AlertApplicationService alertApplicationService;

    public AlertController(AlertApplicationService alertApplicationService) {
        this.alertApplicationService = alertApplicationService;
    }

    /**
     * 方法功能描述：创建告警
     *
     * @param request 告警创建请求
     * @return 告警信息
     */
    @Operation(summary = "创建告警", description = "创建告警事件。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.ALERT_CREATE)
    @PostMapping
    public ApiResponse<AlertDTO> createAlert(
            @Parameter(description = "告警创建请求", required = true) @Valid @RequestBody
                    AlertCreateRequest request) {
        return ApiResponse.success(alertApplicationService.createAlert(request));
    }

    /**
     * 方法功能描述：按告警级别查询告警列表
     *
     * @param alertLevel 告警级别
     * @return 告警列表
     */
    @Operation(summary = "按级别查询告警", description = "按告警级别查询告警列表。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.ALERT_LIST_BY_LEVEL)
    @GetMapping(params = "alertLevel")
    public ApiResponse<List<AlertDTO>> listAlertsByLevel(
            @Parameter(description = "告警级别", required = true) @RequestParam("alertLevel")
                    String alertLevel) {
        return ApiResponse.success(alertApplicationService.listAlertsByLevel(alertLevel));
    }

    @Operation(summary = "获取告警事件列表", description = "获取告警事件分页列表。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.ALERT_LIST)
    @GetMapping
    public ApiResponse<AlertEventPageDTO> listAlertEvents(
            @Parameter(description = "页码", required = false)
                    @RequestParam(value = "page", required = false)
                    Integer page,
            @Parameter(description = "每页数量", required = false)
                    @RequestParam(value = "size", required = false)
                    Integer size,
            @Parameter(description = "来源模块", required = false)
                    @RequestParam(value = "sourceModule", required = false)
                    String sourceModule,
            @Parameter(description = "告警等级", required = false)
                    @RequestParam(value = "level", required = false)
                    Integer level,
            @Parameter(description = "告警状态", required = false)
                    @RequestParam(value = "status", required = false)
                    Integer status,
            @Parameter(description = "是否活跃", required = false)
                    @RequestParam(value = "isActive", required = false)
                    Boolean isActive) {
        return ApiResponse.success(
                alertApplicationService.listAlertEvents(
                        page, size, sourceModule, level, status, isActive));
    }

    /**
     * 方法功能描述：查询告警详情
     *
     * @param eventId 告警事件ID
     * @return 告警信息
     */
    @Operation(summary = "查询告警详情", description = "查询告警详情。成功返回200；告警不存在返回404；服务器异常返回500。")
    @GetMapping("/{eventId}")
    public ApiResponse<AlertDTO> getAlert(
            @Parameter(description = "告警事件ID", required = true) @PathVariable("eventId")
                    Long eventId) {
        return ApiResponse.success(alertApplicationService.getAlert(eventId));
    }

    @Operation(summary = "确认告警", description = "快速确认告警。成功返回200；告警不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.ALERT_ACK)
    @PostMapping("/{eventId}/ack")
    public ApiResponse<Void> acknowledgeAlert(
            @Parameter(description = "告警事件ID", required = true) @PathVariable("eventId")
                    Long eventId) {
        alertApplicationService.acknowledgeAlert(eventId);
        return ApiResponse.success();
    }

    @Operation(summary = "更新告警状态", description = "更新告警状态。成功返回200；告警不存在返回404；参数错误返回400；服务器异常返回500。")
    @PutMapping("/{eventId}/status")
    public ApiResponse<Void> updateAlertStatus(
            @Parameter(description = "告警事件ID", required = true) @PathVariable("eventId")
                    Long eventId,
            @Parameter(description = "告警状态更新请求", required = true) @Valid @RequestBody
                    UpdateAlertStatusRequest request) {
        alertApplicationService.updateAlertStatus(eventId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "获取告警处理日志列表", description = "获取告警处理日志列表。成功返回200；服务器异常返回500。")
    @GetMapping("/{eventId}/logs")
    public ApiResponse<AlertHandleLogPageDTO> listAlertHandleLogs(
            @Parameter(description = "告警事件ID", required = true) @PathVariable("eventId")
                    Long eventId) {
        return ApiResponse.success(alertApplicationService.listAlertHandleLogs(eventId));
    }

    @Operation(
            summary = "获取告警统计信息",
            description = "获取告警统计信息，包括总数、状态分布、级别分布、类型分布等。成功返回200；服务器异常返回500。")
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getAlertStatistics() {
        return ApiResponse.success(alertApplicationService.getAlertStatistics());
    }
}

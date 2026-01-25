package com.huicang.wise.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.alert.AlertEventPageDTO;
import com.huicang.wise.application.alert.AlertHandleLogPageDTO;
import com.huicang.wise.application.alert.UpdateAlertStatusRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 类功能描述：告警管理控制层
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现告警接口
 */
@Api(tags = "告警管理接口")
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
    @ApiOperation(
            value = "创建告警",
            notes = "创建告警事件。成功返回200；参数错误返回400；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.ALERT_CREATE)
    @PostMapping
    public ApiResponse<AlertDTO> createAlert(
            @ApiParam(value = "告警创建请求", required = true)
            @RequestBody AlertCreateRequest request) {
        return ApiResponse.success(alertApplicationService.createAlert(request));
    }

    /**
     * 方法功能描述：按告警级别查询告警列表
     *
     * @param alertLevel 告警级别
     * @return 告警列表
     */
    @ApiOperation(
            value = "按级别查询告警",
            notes = "按告警级别查询告警列表。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.ALERT_LIST_BY_LEVEL)
    @GetMapping(params = "alertLevel")
    public ApiResponse<List<AlertDTO>> listAlertsByLevel(
            @ApiParam(value = "告警级别", required = true)
            @RequestParam("alertLevel") String alertLevel) {
        return ApiResponse.success(alertApplicationService.listAlertsByLevel(alertLevel));
    }

    @ApiOperation(
            value = "获取告警事件列表",
            notes = "获取告警事件分页列表。成功返回200；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.ALERT_LIST)
    @GetMapping
    public ApiResponse<AlertEventPageDTO> listAlertEvents(
            @ApiParam(value = "页码", required = false)
            @RequestParam(value = "page", required = false) Integer page,
            @ApiParam(value = "每页数量", required = false)
            @RequestParam(value = "size", required = false) Integer size,
            @ApiParam(value = "来源模块", required = false)
            @RequestParam(value = "sourceModule", required = false) String sourceModule,
            @ApiParam(value = "告警等级", required = false)
            @RequestParam(value = "level", required = false) Integer level,
            @ApiParam(value = "告警状态", required = false)
            @RequestParam(value = "status", required = false) Integer status,
            @ApiParam(value = "是否活跃", required = false)
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        return ApiResponse.success(alertApplicationService.listAlertEvents(page, size, sourceModule, level, status,
                isActive));
    }

    /**
     * 方法功能描述：查询告警详情
     *
     * @param eventId 告警事件ID
     * @return 告警信息
     */
    @ApiOperation(
            value = "查询告警详情",
            notes = "查询告警详情。成功返回200；告警不存在返回404；服务器异常返回500。"
    )
    @GetMapping("/{eventId}")
    public ApiResponse<AlertDTO> getAlert(
            @ApiParam(value = "告警事件ID", required = true)
            @PathVariable("eventId") Long eventId) {
        return ApiResponse.success(alertApplicationService.getAlert(eventId));
    }

    @ApiOperation(
            value = "确认告警",
            notes = "快速确认告警。成功返回200；告警不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.ALERT_ACK)
    @PostMapping("/{eventId}/ack")
    public ApiResponse<Void> acknowledgeAlert(
            @ApiParam(value = "告警事件ID", required = true)
            @PathVariable("eventId") Long eventId) {
        alertApplicationService.acknowledgeAlert(eventId);
        return ApiResponse.success();
    }

    @ApiOperation(
            value = "更新告警状态",
            notes = "更新告警状态。成功返回200；告警不存在返回404；参数错误返回400；服务器异常返回500。"
    )
    @PutMapping("/{eventId}/status")
    public ApiResponse<Void> updateAlertStatus(
            @ApiParam(value = "告警事件ID", required = true)
            @PathVariable("eventId") Long eventId,
            @ApiParam(value = "告警状态更新请求", required = true)
            @RequestBody UpdateAlertStatusRequest request) {
        alertApplicationService.updateAlertStatus(eventId, request);
        return ApiResponse.success();
    }

    @ApiOperation(
            value = "获取告警处理日志列表",
            notes = "获取告警处理日志列表。成功返回200；服务器异常返回500。"
    )
    @GetMapping("/{eventId}/logs")
    public ApiResponse<AlertHandleLogPageDTO> listAlertHandleLogs(
            @ApiParam(value = "告警事件ID", required = true)
            @PathVariable("eventId") Long eventId) {
        return ApiResponse.success(alertApplicationService.listAlertHandleLogs(eventId));
    }
}

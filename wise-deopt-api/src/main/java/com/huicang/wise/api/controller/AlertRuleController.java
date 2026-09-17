package com.huicang.wise.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.alert.AlertRuleService;
import com.huicang.wise.application.alert.RfidVideoConsistencyRequest;
import com.huicang.wise.application.alert.UnauthorizedMoveRequest;
import com.huicang.wise.application.alert.DeviceOfflineRequest;
import com.huicang.wise.application.alert.InventoryAbnormalRequest;
import com.huicang.wise.common.api.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * 类功能描述：告警规则控制层
 *
 * @author WiseDepot
 * @version 0.1.17
 * @since 2026-02-27
 */
@Tag(name = "告警规则接口")
@RestController
@RequestMapping("/api/alert-rules")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final AlertApplicationService alertApplicationService;

    public AlertRuleController(AlertRuleService alertRuleService, AlertApplicationService alertApplicationService) {
        this.alertRuleService = alertRuleService;
        this.alertApplicationService = alertApplicationService;
    }

    /**
     * 方法功能描述：创建RFID-视频一致性告警
     *
     * @param request RFID-视频一致性请求
     * @return 告警信息
     */
    @Operation(summary = "创建RFID-视频一致性告警", description = "当RFID识别数量与视频识别数量不一致时创建告警。成功返回200；参数错误返回400；服务器异常返回500。")
    @PostMapping("/rfid-video-consistency")
    public ApiResponse<AlertDTO> createRfidVideoConsistencyAlert(
            @Parameter(description = "RFID-视频一致性请求", required = true)
            @RequestBody RfidVideoConsistencyRequest request) {
        AlertDTO alert = alertRuleService.createRfidVideoConsistencyAlert(
                request.getRfidCount(), request.getVideoCount(), request.getLocation());
        return ApiResponse.success(alert);
    }

    /**
     * 方法功能描述：创建违规移动告警
     *
     * @param request 违规移动请求
     * @return 告警信息
     */
    @Operation(summary = "创建违规移动告警", description = "当产品违规移动时创建告警。成功返回200；参数错误返回400；服务器异常返回500。")
    @PostMapping("/unauthorized-move")
    public ApiResponse<AlertDTO> createUnauthorizedMoveAlert(
            @Parameter(description = "违规移动请求", required = true)
            @RequestBody UnauthorizedMoveRequest request) {
        AlertDTO alert = alertRuleService.createUnauthorizedMoveAlert(
                request.getProductId(), request.getProductName(), request.getFromLocation(),
                request.getToLocation(), request.getReason());
        return ApiResponse.success(alert);
    }

    /**
     * 方法功能描述：创建设备离线告警
     *
     * @param request 设备离线请求
     * @return 告警信息
     */
    @Operation(summary = "创建设备离线告警", description = "当设备离线时创建告警。成功返回200；参数错误返回400；服务器异常返回500。")
    @PostMapping("/device-offline")
    public ApiResponse<AlertDTO> createDeviceOfflineAlert(
            @Parameter(description = "设备离线请求", required = true)
            @RequestBody DeviceOfflineRequest request) {
        AlertDTO alert = alertRuleService.createDeviceOfflineAlert(
                request.getDeviceId(), request.getDeviceName(), request.getDeviceType(), request.getLastHeartbeatTime());
        return ApiResponse.success(alert);
    }

    /**
     * 方法功能描述：创建库存异常告警
     *
     * @param request 库存异常请求
     * @return 告警信息
     */
    @Operation(summary = "创建库存异常告警", description = "当库存数量异常时创建告警。成功返回200；参数错误返回400；服务器异常返回500。")
    @PostMapping("/inventory-abnormal")
    public ApiResponse<AlertDTO> createInventoryAbnormalAlert(
            @Parameter(description = "库存异常请求", required = true)
            @RequestBody InventoryAbnormalRequest request) {
        AlertDTO alert = alertRuleService.createInventoryAbnormalAlert(
                request.getProductId(), request.getProductName(), request.getExpectedQuantity(),
                request.getActualQuantity(), request.getWarehouseName());
        return ApiResponse.success(alert);
    }
}

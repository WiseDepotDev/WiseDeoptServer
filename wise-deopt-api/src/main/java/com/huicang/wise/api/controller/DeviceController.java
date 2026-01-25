package com.huicang.wise.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huicang.wise.application.device.DeviceApplicationService;
import com.huicang.wise.application.device.DeviceCreateRequest;
import com.huicang.wise.application.device.DeviceDTO;
import com.huicang.wise.application.device.DeviceHeartbeatLogDTO;
import com.huicang.wise.application.device.DeviceUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 类功能描述：设备管理控制器
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Api(tags = "设备管理")
@RestController
@RequestMapping("/device")
public class DeviceController {

    private final DeviceApplicationService deviceApplicationService;

    public DeviceController(DeviceApplicationService deviceApplicationService) {
        this.deviceApplicationService = deviceApplicationService;
    }

    /**
     * 方法功能描述：创建设备
     *
     * @param request 创建设备请求参数
     * @return 创建后的设备信息
     */
    @ApiOperation("创建设备")
    @ApiPacketType(PacketType.DEVICE_CREATE)
    @PostMapping("/create")
    public ApiResponse<DeviceDTO> createDevice(
            @ApiParam(value = "创建设备请求参数", required = true)
            @RequestBody DeviceCreateRequest request) {
        return ApiResponse.success(deviceApplicationService.createDevice(request));
    }

    /**
     * 方法功能描述：更新设备信息
     *
     * @param request 更新设备请求参数
     * @return 更新后的设备信息
     */
    @ApiOperation("更新设备")
    @ApiPacketType(PacketType.DEVICE_UPDATE)
    @PutMapping("/update")
    public ApiResponse<DeviceDTO> updateDevice(
            @ApiParam(value = "更新设备请求参数", required = true)
            @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.success(deviceApplicationService.updateDevice(request));
    }

    /**
     * 方法功能描述：获取设备详情
     *
     * @param id 设备ID
     * @return 设备详情
     */
    @ApiOperation("获取设备详情")
    @ApiPacketType(PacketType.DEVICE_DETAIL)
    @GetMapping("/{id}")
    public ApiResponse<DeviceDTO> getDevice(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("id") Long id) {
        return ApiResponse.success(deviceApplicationService.getDevice(id));
    }

    /**
     * 方法功能描述：获取设备列表
     *
     * @param type 设备类型（可选）
     * @param status 设备状态（可选）
     * @return 设备列表
     */
    @ApiOperation("获取设备列表")
    @ApiPacketType(PacketType.DEVICE_LIST)
    @GetMapping("/list")
    public ApiResponse<List<DeviceDTO>> listDevices(
            @ApiParam(value = "设备类型") @RequestParam(required = false) Integer type,
            @ApiParam(value = "设备状态") @RequestParam(required = false) Integer status) {
        if (type != null) {
            return ApiResponse.success(deviceApplicationService.listDevicesByType(type));
        }
        if (status != null) {
            return ApiResponse.success(deviceApplicationService.listDevicesByStatus(status));
        }
        return ApiResponse.success(deviceApplicationService.listDevices());
    }

    /**
     * 方法功能描述：删除设备
     *
     * @param id 设备ID
     * @return 操作结果
     */
    @ApiOperation("删除设备")
    @ApiPacketType(PacketType.DEVICE_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDevice(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("id") Long id) {
        deviceApplicationService.deleteDevice(id);
        return ApiResponse.success(null);
    }

    /**
     * 方法功能描述：上报心跳
     *
     * @param deviceId 设备ID
     * @param remark 备注
     * @return 操作结果
     */
    @ApiOperation("上报心跳")
    @PostMapping("/heartbeat")
    public ApiResponse<Void> recordHeartbeat(
            @ApiParam(value = "设备ID", required = true) @RequestParam Long deviceId,
            @ApiParam(value = "备注") @RequestParam(required = false) String remark) {
        deviceApplicationService.recordHeartbeat(deviceId, remark);
        return ApiResponse.success(null);
    }

    /**
     * 方法功能描述：获取设备心跳日志
     *
     * @param deviceId 设备ID
     * @return 心跳日志列表
     */
    @ApiOperation("获取设备心跳日志")
    @GetMapping("/heartbeat-logs/{deviceId}")
    public ApiResponse<List<DeviceHeartbeatLogDTO>> listHeartbeatLogs(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId) {
        return ApiResponse.success(deviceApplicationService.listHeartbeatLogs(deviceId));
    }
}

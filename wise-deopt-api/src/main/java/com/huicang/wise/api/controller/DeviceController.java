package com.huicang.wise.api.controller;

import com.huicang.wise.application.device.DeviceApplicationService;
import com.huicang.wise.application.device.DeviceCreateRequest;
import com.huicang.wise.application.device.DeviceHeartbeatRequest;
import com.huicang.wise.application.device.DeviceDTO;
import com.huicang.wise.application.device.DeviceUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 类功能描述：设备管理控制器
 *
 * @author xingchentye
 * @date 2026-02-27
 * @modified xingchentye 2026-02-27 实现版本0.1.14功能：设备CRUD、心跳接收、状态监控
 */
@Tag(name = "设备管理接口")
@RestController
@RequestMapping({"/api/device", "/device"})
public class DeviceController {

    private final DeviceApplicationService deviceApplicationService;

    public DeviceController(DeviceApplicationService deviceApplicationService) {
        this.deviceApplicationService = deviceApplicationService;
    }

    @Operation(summary = "创建设备", description = "创建设备。成功返回200；参数错误返回400；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_CREATE)
    @PostMapping
    public ApiResponse<DeviceDTO> createDevice(
            @Parameter(description = "设备创建请求", required = true)
            @RequestBody DeviceCreateRequest request) {
        return ApiResponse.success(deviceApplicationService.createDevice(request));
    }

    @Operation(summary = "更新设备", description = "更新设备信息。成功返回200；设备不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_UPDATE)
    @PutMapping("/{deviceId}")
    public ApiResponse<DeviceDTO> updateDevice(
            @Parameter(description = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId,
            @Parameter(description = "设备更新请求", required = true)
            @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.success(deviceApplicationService.updateDevice(deviceId, request));
    }

    @Operation(summary = "删除设备", description = "删除设备。成功返回200；设备不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_DELETE)
    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> deleteDevice(
            @Parameter(description = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId) {
        deviceApplicationService.deleteDevice(deviceId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取设备详情", description = "根据设备ID获取设备详情。成功返回200；设备不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_DETAIL)
    @GetMapping("/{deviceId}")
    public ApiResponse<DeviceDTO> getDevice(
            @Parameter(description = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId) {
        return ApiResponse.success(deviceApplicationService.getDevice(deviceId));
    }

    @Operation(summary = "根据设备编码查询", description = "根据设备编码查询设备详情。成功返回200；设备不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_DETAIL)
    @GetMapping("/code/{deviceCode}")
    public ApiResponse<DeviceDTO> getDeviceByCode(
            @Parameter(description = "设备编码", required = true)
            @PathVariable("deviceCode") String deviceCode) {
        return ApiResponse.success(deviceApplicationService.getDeviceByCode(deviceCode));
    }

    @Operation(summary = "查询设备列表", description = "查询设备列表，支持按设备类型、状态、关键字筛选。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_LIST)
    @GetMapping
    public ApiResponse<List<DeviceDTO>> listDevices(
            @Parameter(description = "设备类型（0:RFID读写器 1:摄像头 2:巡检小车，可选）", required = false)
            @RequestParam(value = "deviceType", required = false) Short deviceType,
            @Parameter(description = "设备状态（0:离线 1:在线 2:故障，可选）", required = false)
            @RequestParam(value = "deviceStatus", required = false) Short deviceStatus,
            @Parameter(description = "是否启用（可选）", required = false)
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @Parameter(description = "搜索关键字（可选）", required = false)
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(deviceApplicationService.listDevices(deviceType, deviceStatus, enabled, keyword));
    }

    @Operation(summary = "接收设备心跳", description = "接收设备心跳，更新设备在线状态。成功返回200；设备不存在返回404；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_HEARTBEAT)
    @PostMapping("/heartbeat")
    public ApiResponse<Void> receiveHeartbeat(
            @Parameter(description = "设备编码", required = false)
            @RequestBody(required = false) DeviceHeartbeatRequest request,
            @Parameter(hidden = true)
            @RequestParam(value = "deviceCode", required = false) String deviceCodeParam) {
        
        String deviceCode = null;
        if (request != null && request.getDeviceCode() != null) {
            deviceCode = request.getDeviceCode();
        } else if (deviceCodeParam != null) {
            deviceCode = deviceCodeParam;
        }

        if (deviceCode == null) {
            return ApiResponse.error(400, "Device code is required");
        }

        deviceApplicationService.receiveHeartbeat(deviceCode);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取设备统计信息", description = "获取设备在线统计信息。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_STATISTICS)
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getDeviceStatistics() {
        return ApiResponse.success(deviceApplicationService.getDeviceStatistics());
    }

    @Operation(summary = "获取设备配置", description = "获取设备配置参数。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_CONFIG)
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getDeviceConfig(
            @Parameter(description = "设备编码", required = true)
            @RequestParam("deviceId") String deviceCode,
            @Parameter(description = "当前版本", required = true)
            @RequestParam("version") String version) {
        return ApiResponse.success(deviceApplicationService.getConfig(deviceCode, version));
    }

    @Operation(summary = "上传设备日志", description = "上传设备日志文件。成功返回200；服务器异常返回500。")
    @ApiPacketType(PacketType.DEVICE_LOG_UPLOAD)
    @PostMapping("/logs/upload")
    public ApiResponse<Void> uploadDeviceLogs(
            @Parameter(description = "设备编码", required = true)
            @RequestParam("deviceId") String deviceCode,
            @RequestBody String logs) {
        deviceApplicationService.uploadLogs(deviceCode, logs);
        return ApiResponse.success(null);
    }
}

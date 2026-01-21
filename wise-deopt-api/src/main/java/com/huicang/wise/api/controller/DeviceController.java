package com.huicang.wise.api.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "设备控制器")
@RestController
@RequestMapping("/device")
public class DeviceController {

    private final DeviceApplicationService deviceApplicationService;

    public DeviceController(DeviceApplicationService deviceApplicationService) {
        this.deviceApplicationService = deviceApplicationService;
    }

    @ApiOperation(
            value = "创建设备",
            notes = "创建设备信息。成功返回200；参数错误返回400；服务器异常返回500。"
    )
    @PostMapping
    public ApiResponse<DeviceDTO> createDevice(
            @ApiParam(value = "设备创建请求", required = true)
            @RequestBody DeviceCreateRequest request) {
        return ApiResponse.success(deviceApplicationService.createDevice(request));
    }

    @ApiOperation(
            value = "更新设备",
            notes = "更新设备信息。成功返回200；设备不存在返回404；服务器异常返回500。"
    )
    @PutMapping
    public ApiResponse<DeviceDTO> updateDevice(
            @ApiParam(value = "设备更新请求", required = true)
            @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.success(deviceApplicationService.updateDevice(request));
    }

    @ApiOperation(
            value = "获取所有设备",
            notes = "获取设备列表。成功返回200；服务器异常返回500。"
    )
    @GetMapping
    public ApiResponse<List<DeviceDTO>> listDevices() {
        return ApiResponse.success(deviceApplicationService.listDevices());
    }

    @ApiOperation(
            value = "获取设备详情",
            notes = "根据设备ID获取设备信息。成功返回200；设备不存在返回404；服务器异常返回500。"
    )
    @GetMapping("/{deviceId}")
    public ApiResponse<DeviceDTO> getDevice(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId) {
        return ApiResponse.success(deviceApplicationService.getDevice(deviceId));
    }

    @ApiOperation(
            value = "删除设备",
            notes = "删除设备信息。成功返回200；设备不存在返回404；服务器异常返回500。"
    )
    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> deleteDevice(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId) {
        deviceApplicationService.deleteDevice(deviceId);
        return ApiResponse.success();
    }

    @ApiOperation(
            value = "按类型获取设备",
            notes = "根据设备类型获取设备列表。成功返回200；服务器异常返回500。"
    )
    @GetMapping("/type/{type}")
    public ApiResponse<List<DeviceDTO>> listDevicesByType(
            @ApiParam(value = "设备类型", required = true)
            @PathVariable("type") Integer type) {
        return ApiResponse.success(deviceApplicationService.listDevicesByType(type));
    }

    @ApiOperation(
            value = "按状态获取设备",
            notes = "根据设备状态获取设备列表。成功返回200；服务器异常返回500。"
    )
    @GetMapping("/status/{status}")
    public ApiResponse<List<DeviceDTO>> listDevicesByStatus(
            @ApiParam(value = "设备状态", required = true)
            @PathVariable("status") Integer status) {
        return ApiResponse.success(deviceApplicationService.listDevicesByStatus(status));
    }

    @ApiOperation(
            value = "记录设备心跳",
            notes = "记录设备心跳。成功返回200；设备不存在返回404；服务器异常返回500。"
    )
    @PostMapping("/heartbeat/{deviceId}")
    public ApiResponse<Void> recordHeartbeat(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId,
            @ApiParam(value = "备注", required = false)
            @RequestParam(value = "remark", required = false) String remark) {
        deviceApplicationService.recordHeartbeat(deviceId, remark);
        return ApiResponse.success();
    }

    @ApiOperation(
            value = "RFID读写器心跳上报",
            notes = "RFID读写器心跳上报。成功返回200；设备不存在返回404；服务器异常返回500。"
    )
    @PostMapping("/heartbeat/rfid/{deviceId}")
    public ApiResponse<Void> recordRfidHeartbeat(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId,
            @ApiParam(value = "备注", required = false)
            @RequestParam(value = "remark", required = false) String remark) {
        deviceApplicationService.recordHeartbeat(deviceId, remark);
        return ApiResponse.success();
    }

    @ApiOperation(
            value = "摄像头心跳上报",
            notes = "摄像头心跳上报。成功返回200；设备不存在返回404；服务器异常返回500。"
    )
    @PostMapping("/heartbeat/camera/{deviceId}")
    public ApiResponse<Void> recordCameraHeartbeat(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId,
            @ApiParam(value = "备注", required = false)
            @RequestParam(value = "remark", required = false) String remark) {
        deviceApplicationService.recordHeartbeat(deviceId, remark);
        return ApiResponse.success();
    }

    @ApiOperation(
            value = "获取设备心跳记录",
            notes = "获取设备心跳记录列表。成功返回200；服务器异常返回500。"
    )
    @GetMapping("/heartbeat/{deviceId}")
    public ApiResponse<List<DeviceHeartbeatLogDTO>> listHeartbeatLogs(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId) {
        return ApiResponse.success(deviceApplicationService.listHeartbeatLogs(deviceId));
    }

    @ApiOperation(
            value = "按时间范围获取设备心跳记录",
            notes = "根据时间范围获取设备心跳记录。成功返回200；参数错误返回400；服务器异常返回500。"
    )
    @GetMapping("/heartbeat/{deviceId}/range")
    public ApiResponse<List<DeviceHeartbeatLogDTO>> listHeartbeatLogsByRange(
            @ApiParam(value = "设备ID", required = true)
            @PathVariable("deviceId") Long deviceId,
            @ApiParam(value = "开始时间", required = true)
            @RequestParam("startTime") String startTime,
            @ApiParam(value = "结束时间", required = true)
            @RequestParam("endTime") String endTime) {
        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);
        return ApiResponse.success(deviceApplicationService.listHeartbeatLogs(deviceId, start, end));
    }

    private LocalDateTime parseTime(String value) throws BusinessException {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "时间参数不能为空");
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ex) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "时间格式不正确");
            }
        }
    }
}

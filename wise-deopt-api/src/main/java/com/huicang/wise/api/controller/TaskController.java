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

import com.huicang.wise.application.task.TaskApplicationService;
import com.huicang.wise.application.task.TaskCreateRequest;
import com.huicang.wise.application.task.TaskDTO;
import com.huicang.wise.application.task.TaskUpdateRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.protocol.ApiPacketType;
import com.huicang.wise.common.protocol.PacketType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 类功能描述：任务管理控制层
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Api(tags = "任务管理接口")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @ApiOperation(
            value = "创建任务",
            notes = "创建任务。成功返回200；参数错误返回400；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.TASK_CREATE)
    @PostMapping
    public ApiResponse<TaskDTO> createTask(
            @ApiParam(value = "任务创建请求", required = true)
            @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskApplicationService.createTask(request));
    }

    @ApiOperation(
            value = "更新任务",
            notes = "更新任务。成功返回200；任务不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.TASK_UPDATE)
    @PutMapping("/{taskId}")
    public ApiResponse<TaskDTO> updateTask(
            @ApiParam(value = "任务ID", required = true)
            @PathVariable("taskId") Long taskId,
            @ApiParam(value = "任务更新请求", required = true)
            @RequestBody TaskUpdateRequest request) {
        request.setTaskId(taskId);
        return ApiResponse.success(taskApplicationService.updateTask(request));
    }

    @ApiOperation(
            value = "查询任务详情",
            notes = "查询任务详情。成功返回200；任务不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.TASK_DETAIL)
    @GetMapping("/{taskId}")
    public ApiResponse<TaskDTO> getTask(
            @ApiParam(value = "任务ID", required = true)
            @PathVariable("taskId") Long taskId) {
        return ApiResponse.success(taskApplicationService.getTask(taskId));
    }

    @ApiOperation(
            value = "获取任务列表",
            notes = "获取任务列表。支持按状态或类型过滤。"
    )
    @ApiPacketType(PacketType.TASK_LIST)
    @GetMapping
    public ApiResponse<List<TaskDTO>> listTasks(
            @ApiParam(value = "任务状态", required = false)
            @RequestParam(value = "status", required = false) Integer status,
            @ApiParam(value = "任务类型", required = false)
            @RequestParam(value = "type", required = false) Integer type) {
        if (status != null) {
            return ApiResponse.success(taskApplicationService.listTasksByStatus(status));
        }
        if (type != null) {
            return ApiResponse.success(taskApplicationService.listTasksByType(type));
        }
        return ApiResponse.success(taskApplicationService.listTasks());
    }

    @ApiOperation(
            value = "删除任务",
            notes = "删除任务。成功返回200；任务不存在返回404；服务器异常返回500。"
    )
    @ApiPacketType(PacketType.TASK_DELETE)
    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(
            @ApiParam(value = "任务ID", required = true)
            @PathVariable("taskId") Long taskId) {
        taskApplicationService.deleteTask(taskId);
        return ApiResponse.success(null);
    }
}

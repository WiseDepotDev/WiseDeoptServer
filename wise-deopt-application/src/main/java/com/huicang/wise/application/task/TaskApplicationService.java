package com.huicang.wise.application.task;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.task.TaskJpaEntity;
import com.huicang.wise.infrastructure.repository.task.TaskRepository;

/**
 * 类功能描述：任务应用服务
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Service
public class TaskApplicationService {

    private final TaskRepository taskRepository;

    public TaskApplicationService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * 方法功能描述：创建任务
     *
     * @param request 任务创建请求
     * @return 任务DTO
     */
    @Transactional
    public TaskDTO createTask(TaskCreateRequest request) {
        if (request.getTaskName() == null || request.getTaskName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务名称不能为空");
        }
        if (request.getTaskType() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务类型不能为空");
        }

        TaskJpaEntity entity = new TaskJpaEntity();
        entity.setTaskId(System.currentTimeMillis());
        entity.setTaskCode(generateTaskCode());
        entity.setTaskName(request.getTaskName());
        entity.setTaskType(request.getTaskType());
        entity.setStatus(0); // 待开始
        entity.setPlannedStartTime(request.getPlannedStartTime());
        entity.setCreatedBy(request.getCreatedBy());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setRemark(request.getRemark());

        TaskJpaEntity saved = taskRepository.save(entity);
        return toDTO(saved);
    }

    /**
     * 方法功能描述：更新任务
     *
     * @param request 任务更新请求
     * @return 任务DTO
     */
    @Transactional
    public TaskDTO updateTask(TaskUpdateRequest request) {
        if (request.getTaskId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        TaskJpaEntity entity = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));

        if (request.getTaskName() != null) {
            entity.setTaskName(request.getTaskName());
        }
        if (request.getPlannedStartTime() != null) {
            entity.setPlannedStartTime(request.getPlannedStartTime());
        }
        if (request.getRemark() != null) {
            entity.setRemark(request.getRemark());
        }
        
        // 状态流转简单处理，实际可能需要状态机
        if (request.getStatus() != null) {
            // 如果变成进行中
            if (request.getStatus() == 1 && entity.getStatus() == 0) {
                entity.setActualStartTime(LocalDateTime.now());
            }
            // 如果变成已完成
            if (request.getStatus() == 2 && entity.getStatus() != 2) {
                entity.setActualEndTime(LocalDateTime.now());
            }
            entity.setStatus(request.getStatus());
        }

        TaskJpaEntity saved = taskRepository.save(entity);
        return toDTO(saved);
    }

    public TaskDTO getTask(Long taskId) {
        TaskJpaEntity entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
        return toDTO(entity);
    }

    public List<TaskDTO> listTasks() {
        return taskRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TaskDTO> listTasksByStatus(Integer status) {
        return taskRepository.findByStatus(status).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TaskDTO> listTasksByType(Integer type) {
        return taskRepository.findByTaskType(type).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO startTask(Long taskId) {
        TaskJpaEntity entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
        
        if (entity.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务已经在进行中");
        }
        if (entity.getStatus() == 2 || entity.getStatus() == 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务已结束，无法开始");
        }

        entity.setStatus(1); // 进行中
        if (entity.getActualStartTime() == null) {
            entity.setActualStartTime(LocalDateTime.now());
        }
        TaskJpaEntity saved = taskRepository.save(entity);
        return toDTO(saved);
    }

    @Transactional
    public TaskDTO pauseTask(Long taskId) {
        TaskJpaEntity entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));

        if (entity.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务未在进行中，无法暂停");
        }

        entity.setStatus(4); // 暂停
        TaskJpaEntity saved = taskRepository.save(entity);
        return toDTO(saved);
    }

    @Transactional
    public TaskDTO completeTask(Long taskId) {
        TaskJpaEntity entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));

        if (entity.getStatus() == 2) {
            return toDTO(entity);
        }

        entity.setStatus(2); // 已完成
        entity.setActualEndTime(LocalDateTime.now());
        TaskJpaEntity saved = taskRepository.save(entity);
        return toDTO(saved);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        taskRepository.deleteById(taskId);
    }

    private TaskDTO toDTO(TaskJpaEntity entity) {
        TaskDTO dto = new TaskDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setTaskCode(entity.getTaskCode());
        dto.setTaskName(entity.getTaskName());
        dto.setTaskType(entity.getTaskType());
        dto.setTaskTypeDesc(resolveTypeDesc(entity.getTaskType()));
        dto.setStatus(entity.getStatus());
        dto.setStatusDesc(resolveStatusDesc(entity.getStatus()));
        dto.setPlannedStartTime(entity.getPlannedStartTime());
        dto.setActualStartTime(entity.getActualStartTime());
        dto.setActualEndTime(entity.getActualEndTime());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    private String generateTaskCode() {
        return "T" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String resolveTypeDesc(Integer type) {
        if (type == null) return "";
        if (type == 0) return "盘点任务";
        if (type == 1) return "巡检任务";
        return "未知类型";
    }

    private String resolveStatusDesc(Integer status) {
        if (status == null) return "";
        if (status == 0) return "待开始";
        if (status == 1) return "进行中";
        if (status == 2) return "已完成";
        if (status == 3) return "已取消";
        return "未知状态";
    }
}

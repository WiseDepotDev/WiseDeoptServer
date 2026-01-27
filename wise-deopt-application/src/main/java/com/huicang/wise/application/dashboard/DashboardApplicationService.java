package com.huicang.wise.application.dashboard;

import com.huicang.wise.application.alert.AlertEventSummaryDTO;
import com.huicang.wise.application.task.TaskDTO;
import com.huicang.wise.infrastructure.repository.alert.AlertEventJpaEntity;
import com.huicang.wise.infrastructure.repository.alert.AlertEventRepository;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.repository.task.TaskJpaEntity;
import com.huicang.wise.infrastructure.repository.task.TaskRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardApplicationService {

    private static final String DASHBOARD_KPI_KEY = "dashboard:kpi";
    private static final String INSPECTION_PROGRESS_KEY = "inspection:progress";

    private final InventoryRepository inventoryRepository;
    private final AlertEventRepository alertEventRepository;
    private final TaskRepository taskRepository;
    private final DeviceCoreRepository deviceCoreRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public DashboardApplicationService(InventoryRepository inventoryRepository,
                                       AlertEventRepository alertEventRepository,
                                       TaskRepository taskRepository,
                                       DeviceCoreRepository deviceCoreRepository,
                                       StringRedisTemplate stringRedisTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.alertEventRepository = alertEventRepository;
        this.taskRepository = taskRepository;
        this.deviceCoreRepository = deviceCoreRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public DashboardSummaryDTO getSummary() {
        DashboardSummaryDTO dto = new DashboardSummaryDTO();

        // 1. 获取基础KPI（优先从缓存）
        DashboardSummaryDTO cached = parseCache(stringRedisTemplate.opsForValue().get(DASHBOARD_KPI_KEY));
        if (cached != null) {
            dto.setInventoryTotal(cached.getInventoryTotal());
            dto.setTodayAlertCount(cached.getTodayAlertCount());
            dto.setInspectionProgress(cached.getInspectionProgress());
            dto.setDeviceOnlineCount(cached.getDeviceOnlineCount());
        } else {
            long inventoryTotal = calculateInventoryTotal();
            long todayAlertCount = calculateTodayAlertCount();
            int inspectionProgress = readInteger(INSPECTION_PROGRESS_KEY);
            int deviceOnlineCount = (int) deviceCoreRepository.countByStatus(1); // 假设1为在线

            dto.setInventoryTotal(inventoryTotal);
            dto.setTodayAlertCount(todayAlertCount);
            dto.setInspectionProgress(inspectionProgress);
            dto.setDeviceOnlineCount(deviceOnlineCount);

            String cacheValue = inventoryTotal + "|" + todayAlertCount + "|" + inspectionProgress + "|" + deviceOnlineCount;
            stringRedisTemplate.opsForValue().set(DASHBOARD_KPI_KEY, cacheValue, Duration.ofMinutes(1)); // 缓存1分钟
        }

        // 2. 获取未处理告警（实时）
        List<AlertEventJpaEntity> alertEntities = alertEventRepository.findTop5ByStatusOrderByAlertTimeDesc(0);
        dto.setUnprocessedAlerts(alertEntities.stream().map(this::toAlertDTO).collect(Collectors.toList()));

        // 3. 获取当前任务（实时，进行中）
        TaskJpaEntity taskEntity = taskRepository.findFirstByStatusOrderByCreatedAtDesc(1);
        if (taskEntity != null) {
            dto.setCurrentTask(toTaskDTO(taskEntity));
        }

        return dto;
    }

    private long calculateInventoryTotal() {
        List<InventoryJpaEntity> entities = inventoryRepository.findAll();
        return entities.stream()
                .mapToLong(entity -> entity.getQuantity() == null ? 0L : entity.getQuantity())
                .sum();
    }

    private long calculateTodayAlertCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return alertEventRepository.countByAlertTimeBetween(start, end);
    }

    private int readInteger(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private DashboardSummaryDTO parseCache(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            DashboardSummaryDTO dto = new DashboardSummaryDTO();
            dto.setInventoryTotal(Long.parseLong(parts[0]));
            dto.setTodayAlertCount(Long.parseLong(parts[1]));
            dto.setInspectionProgress(Integer.parseInt(parts[2]));
            dto.setDeviceOnlineCount(Integer.parseInt(parts[3]));
            return dto;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private AlertEventSummaryDTO toAlertDTO(AlertEventJpaEntity entity) {
        AlertEventSummaryDTO dto = new AlertEventSummaryDTO();
        dto.setEventId(entity.getEventId());
        dto.setSourceModule(entity.getSourceModule());
        dto.setLevel(entity.getLevel());
        dto.setLevelDescription(entity.getAlertLevel()); // Assume alertLevel stores description or code
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus());
        dto.setIsActive(entity.getIsActive());
        dto.setCreateTime(entity.getAlertTime()); // Use alertTime as createTime
        dto.setExtendedData(entity.getExtendedData());
        return dto;
    }

    private TaskDTO toTaskDTO(TaskJpaEntity entity) {
        TaskDTO dto = new TaskDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setTaskCode(entity.getTaskCode());
        dto.setTaskName(entity.getTaskName());
        dto.setTaskType(entity.getTaskType());
        dto.setStatus(entity.getStatus());
        dto.setPlannedStartTime(entity.getPlannedStartTime());
        dto.setActualStartTime(entity.getActualStartTime());
        dto.setActualEndTime(entity.getActualEndTime());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setRemark(entity.getRemark());
        
        // Populate descriptions
        dto.setTaskTypeDesc(resolveTaskTypeDesc(entity.getTaskType()));
        dto.setStatusDesc(resolveTaskStatusDesc(entity.getStatus()));
        
        return dto;
    }

    private String resolveTaskTypeDesc(Integer type) {
        if (type == null) return "";
        if (type == 0) return "盘点任务";
        if (type == 1) return "巡检任务";
        return "未知类型";
    }

    private String resolveTaskStatusDesc(Integer status) {
        if (status == null) return "";
        if (status == 0) return "待开始";
        if (status == 1) return "进行中";
        if (status == 2) return "已完成";
        if (status == 3) return "已取消";
        return "未知状态";
    }
}

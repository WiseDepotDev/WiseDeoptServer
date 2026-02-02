package com.huicang.wise.application.dashboard;

import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.task.TaskDTO;
import com.huicang.wise.infrastructure.repository.alert.AlertEventJpaEntity;
import com.huicang.wise.infrastructure.repository.alert.AlertEventRepository;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.repository.task.TaskJpaEntity;
import com.huicang.wise.infrastructure.repository.task.TaskRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 类功能描述：看板应用服务
 *
 * @author xingchentye
 * @since 2026-02-02
 */
@Service
public class DashboardApplicationService {

    private final InventoryRepository inventoryRepository;
    private final AlertEventRepository alertEventRepository;
    private final TaskRepository taskRepository;
    private final DeviceCoreRepository deviceCoreRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DASHBOARD_KPI_KEY = "dashboard:kpi";
    private static final String INSPECTION_PROGRESS_KEY = "inspection:progress";

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

    /**
     * 方法功能描述：获取看板摘要数据
     *
     * @return 看板摘要DTO
     */
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary() {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        
        // 尝试从缓存获取 KPI 数据
        String cachedKpi = stringRedisTemplate.opsForValue().get(DASHBOARD_KPI_KEY);
        if (StringUtils.hasText(cachedKpi)) {
            DashboardSummaryDTO kpiDto = parseKpi(cachedKpi);
            summary.setInventoryTotal(kpiDto.getInventoryTotal());
            summary.setTodayAlertCount(kpiDto.getTodayAlertCount());
            summary.setInspectionProgress(kpiDto.getInspectionProgress());
            summary.setDeviceOnlineCount(kpiDto.getDeviceOnlineCount());
        } else {
            // 1. 库存总量 (Inventory Total)
            Long totalInventory = inventoryRepository.sumTotalQuantity();
            summary.setInventoryTotal(totalInventory != null ? totalInventory : 0L);

            // 2. 今日报警数 (Today's Alerts)
            LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            long todayAlertCount = alertEventRepository.countByAlertTimeBetween(todayStart, todayEnd);
            summary.setTodayAlertCount(todayAlertCount);

            // 3. 巡检进度 (Inspection Progress)
            String progressStr = stringRedisTemplate.opsForValue().get(INSPECTION_PROGRESS_KEY);
            int progress = 0;
            if (StringUtils.hasText(progressStr)) {
                try {
                    progress = Integer.parseInt(progressStr);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            summary.setInspectionProgress(progress);

            // 4. 设备在线数 (Online Device Count) - Status 1: Online
            long onlineDeviceCount = deviceCoreRepository.countByStatus(1);
            summary.setDeviceOnlineCount(onlineDeviceCount);

            // 缓存结果 (1分钟)
            String kpiValue = String.format("%d|%d|%d|%d",
                    summary.getInventoryTotal(),
                    summary.getTodayAlertCount(),
                    summary.getInspectionProgress(),
                    summary.getDeviceOnlineCount());
            stringRedisTemplate.opsForValue().set(DASHBOARD_KPI_KEY, kpiValue, 1, TimeUnit.MINUTES);
        }

        // 5. 未处理告警列表 (Top 3-5, Status 0: Pending)
        // 实时查询，不缓存
        List<AlertEventJpaEntity> pendingAlerts = alertEventRepository.findTop5ByStatusOrderByAlertTimeDesc(0);
        summary.setUnprocessedAlerts(pendingAlerts.stream().map(this::toAlertDTO).collect(Collectors.toList()));

        // 6. 当前进行中的任务 (Status 1: In Progress)
        // 实时查询，不缓存
        TaskJpaEntity activeTask = taskRepository.findFirstByStatusOrderByCreatedAtDesc(1);
        if (activeTask != null) {
            summary.setCurrentTask(toTaskDTO(activeTask));
        }

        return summary;
    }

    private DashboardSummaryDTO parseKpi(String kpi) {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        try {
            String[] parts = kpi.split("\\|");
            if (parts.length >= 4) {
                summary.setInventoryTotal(Long.parseLong(parts[0]));
                summary.setTodayAlertCount(Long.parseLong(parts[1]));
                summary.setInspectionProgress(Integer.parseInt(parts[2]));
                summary.setDeviceOnlineCount(Long.parseLong(parts[3]));
            }
        } catch (Exception e) {
            // Ignore
        }
        return summary;
    }

    private AlertDTO toAlertDTO(AlertEventJpaEntity entity) {
        AlertDTO dto = new AlertDTO();
        dto.setEventId(entity.getEventId());
        dto.setTitle(entity.getTitle());
        dto.setAlertType(entity.getAlertType());
        dto.setAlertLevel(entity.getAlertLevel());
        dto.setDescription(entity.getDescription());
        dto.setAlertTime(entity.getAlertTime());
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
        dto.setRemark(entity.getRemark());
        return dto;
    }
}

package com.huicang.wise.application.dashboard;

import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.inspection.InspectionTaskDTO;
import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.inspection.InspectionTask;
import com.huicang.wise.domain.repository.alert.AlertEventRepository;
import com.huicang.wise.domain.repository.device.DeviceCoreRepository;
import com.huicang.wise.domain.repository.inspection.InspectionTaskRepository;
import com.huicang.wise.domain.repository.inventory.InventoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DashboardApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DashboardApplicationService.class);

    private final InventoryRepository inventoryRepository;
    private final AlertEventRepository alertEventRepository;
    private final InspectionTaskRepository inspectionTaskRepository;
    private final DeviceCoreRepository deviceCoreRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DASHBOARD_KPI_KEY = "dashboard:kpi";
    private static final String INSPECTION_PROGRESS_KEY = "inspection:progress";

    public DashboardApplicationService(
            InventoryRepository inventoryRepository,
            AlertEventRepository alertEventRepository,
            InspectionTaskRepository inspectionTaskRepository,
            DeviceCoreRepository deviceCoreRepository,
            StringRedisTemplate stringRedisTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.alertEventRepository = alertEventRepository;
        this.inspectionTaskRepository = inspectionTaskRepository;
        this.deviceCoreRepository = deviceCoreRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary() {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();

        String cachedKpi = stringRedisTemplate.opsForValue().get(DASHBOARD_KPI_KEY);
        if (StringUtils.hasText(cachedKpi)) {
            DashboardSummaryDTO kpiDto = parseKpi(cachedKpi);
            summary.setInventoryTotal(kpiDto.getInventoryTotal());
            summary.setTodayAlertCount(kpiDto.getTodayAlertCount());
            summary.setInspectionProgress(kpiDto.getInspectionProgress());
            summary.setDeviceOnlineCount(kpiDto.getDeviceOnlineCount());
        } else {
            Integer totalInventory = inventoryRepository.sumTotalQuantity();
            summary.setInventoryTotal(totalInventory != null ? totalInventory.longValue() : 0L);

            LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            long todayAlertCount =
                    alertEventRepository.countByCreateTimeBetween(todayStart, todayEnd);
            summary.setTodayAlertCount(todayAlertCount);

            String progressStr = stringRedisTemplate.opsForValue().get(INSPECTION_PROGRESS_KEY);
            int progress = 0;
            if (StringUtils.hasText(progressStr)) {
                try {
                    progress = Integer.parseInt(progressStr);
                } catch (NumberFormatException e) {
                    // 缓存值非法时按 0 处理，但必须留痕（STD-ERR-02：禁止静默吞异常）
                    log.warn("巡检进度缓存值非法，已按 0 处理: value={}", progressStr);
                }
            }
            summary.setInspectionProgress(progress);

            long onlineDeviceCount = deviceCoreRepository.countByStatus((short) 1);
            summary.setDeviceOnlineCount(onlineDeviceCount);

            String kpiValue =
                    String.format(
                            "%d|%d|%d|%d",
                            summary.getInventoryTotal(),
                            summary.getTodayAlertCount(),
                            summary.getInspectionProgress(),
                            summary.getDeviceOnlineCount());
            stringRedisTemplate.opsForValue().set(DASHBOARD_KPI_KEY, kpiValue, 1, TimeUnit.MINUTES);
        }

        List<AlertEvent> pendingAlerts = alertEventRepository.findByStatusOrderByCreateTimeDesc(0);
        summary.setUnprocessedAlerts(
                pendingAlerts.stream().map(this::toAlertDTO).collect(Collectors.toList()));

        InspectionTask activeTask =
                inspectionTaskRepository
                        .findFirstByStatusOrderByCreateTimeDesc((short) 1)
                        .orElse(null);
        if (activeTask != null) {
            summary.setCurrentTask(toInspectionTaskDTO(activeTask));
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
            // 缓存不可用时回退到实时统计，但必须留痕（STD-ERR-02）
            log.warn("读取首页 KPI 缓存失败，已回退实时统计: {}", e.getMessage());
        }
        return summary;
    }

    private AlertDTO toAlertDTO(AlertEvent entity) {
        AlertDTO dto = new AlertDTO();
        dto.setEventId(entity.getEventId());
        dto.setTitle(entity.getTitle());
        dto.setSourceModule(entity.getSourceModule());
        dto.setLevel(entity.getLevel() != null ? entity.getLevel().intValue() : null);
        dto.setMessage(entity.getMessage());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }

    private InspectionTaskDTO toInspectionTaskDTO(InspectionTask entity) {
        InspectionTaskDTO dto = new InspectionTaskDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setDeviceId(entity.getDeviceId());
        dto.setStatus(entity.getStatus());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }

    public List<InspectionTaskDTO> getRecentTasks(int limit) {
        return inspectionTaskRepository.findRecentTasks(limit).stream()
                .map(task -> toInspectionTaskDTO(task))
                .collect(Collectors.toList());
    }
}

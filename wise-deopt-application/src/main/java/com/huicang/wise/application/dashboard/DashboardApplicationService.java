package com.huicang.wise.application.dashboard;

import com.huicang.wise.infrastructure.repository.alert.AlertEventRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardApplicationService {

    private static final String DASHBOARD_KPI_KEY = "dashboard:kpi";
    private static final String INSPECTION_PROGRESS_KEY = "inspection:progress";
    private static final String DEVICE_ONLINE_COUNT_KEY = "device:online:count";

    private final InventoryRepository inventoryRepository;
    private final AlertEventRepository alertEventRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public DashboardApplicationService(InventoryRepository inventoryRepository,
                                       AlertEventRepository alertEventRepository,
                                       StringRedisTemplate stringRedisTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.alertEventRepository = alertEventRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public DashboardSummaryDTO getSummary() {
        DashboardSummaryDTO cached = parseCache(stringRedisTemplate.opsForValue().get(DASHBOARD_KPI_KEY));
        if (cached != null) {
            return cached;
        }

        long inventoryTotal = calculateInventoryTotal();
        long todayAlertCount = calculateTodayAlertCount();
        int inspectionProgress = readInteger(INSPECTION_PROGRESS_KEY);
        int deviceOnlineCount = readInteger(DEVICE_ONLINE_COUNT_KEY);

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setInventoryTotal(inventoryTotal);
        dto.setTodayAlertCount(todayAlertCount);
        dto.setInspectionProgress(inspectionProgress);
        dto.setDeviceOnlineCount(deviceOnlineCount);

        String cacheValue = inventoryTotal + "|" + todayAlertCount + "|" + inspectionProgress + "|" + deviceOnlineCount;
        stringRedisTemplate.opsForValue().set(DASHBOARD_KPI_KEY, cacheValue, Duration.ofMinutes(5));
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
}

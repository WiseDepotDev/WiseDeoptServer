package com.huicang.wise.application.dashboard;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import java.util.concurrent.TimeUnit;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.huicang.wise.infrastructure.repository.alert.AlertEventJpaEntity;
import com.huicang.wise.infrastructure.repository.alert.AlertEventRepository;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.repository.task.TaskJpaEntity;
import com.huicang.wise.infrastructure.repository.task.TaskRepository;

@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AlertEventRepository alertEventRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DeviceCoreRepository deviceCoreRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private DashboardApplicationService dashboardApplicationService;

    @BeforeEach
    void setUp() {
        dashboardApplicationService = new DashboardApplicationService(
            inventoryRepository, 
            alertEventRepository, 
            taskRepository, 
            deviceCoreRepository, 
            stringRedisTemplate
        );
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetSummaryCacheHit() {
        // Mock cache hit
        when(valueOperations.get("dashboard:kpi")).thenReturn("100|5|80|10");

        // Mock database calls for real-time data
        when(alertEventRepository.findTop5ByStatusOrderByAlertTimeDesc(0)).thenReturn(Collections.emptyList());
        when(taskRepository.findFirstByStatusOrderByCreatedAtDesc(1)).thenReturn(null);

        DashboardSummaryDTO summary = dashboardApplicationService.getSummary();

        assertNotNull(summary);
        assertEquals(100L, summary.getInventoryTotal());
        assertEquals(5L, summary.getTodayAlertCount());
        assertEquals(80, summary.getInspectionProgress());
        assertEquals(10, summary.getDeviceOnlineCount());
        
        // Verify no DB calls for cached metrics
        verify(inventoryRepository, never()).findAll();
        verify(alertEventRepository, never()).countByAlertTimeBetween(any(), any());
    }

    @Test
    void testGetSummaryCacheMiss() {
        // Mock cache miss
        when(valueOperations.get("dashboard:kpi")).thenReturn(null);
        when(valueOperations.get("inspection:progress")).thenReturn("75");
        // when(valueOperations.get("device:online:count")).thenReturn("15"); // Removed

        // Mock DB calls
        when(deviceCoreRepository.countByStatus(1)).thenReturn(15L);

        when(inventoryRepository.sumTotalQuantity()).thenReturn(110L);

        when(alertEventRepository.countByAlertTimeBetween(any(), any())).thenReturn(3L);

        AlertEventJpaEntity alert = new AlertEventJpaEntity();
        alert.setEventId(1L);
        alert.setTitle("Test Alert");
        when(alertEventRepository.findTop5ByStatusOrderByAlertTimeDesc(0)).thenReturn(Collections.singletonList(alert));

        TaskJpaEntity task = new TaskJpaEntity();
        task.setTaskId(1L);
        task.setTaskName("Test Task");
        when(taskRepository.findFirstByStatusOrderByCreatedAtDesc(1)).thenReturn(task);

        DashboardSummaryDTO summary = dashboardApplicationService.getSummary();

        assertNotNull(summary);
        assertEquals(110L, summary.getInventoryTotal());
        assertEquals(3L, summary.getTodayAlertCount());
        assertEquals(75, summary.getInspectionProgress());
        assertEquals(15, summary.getDeviceOnlineCount());
        
        assertNotNull(summary.getUnprocessedAlerts());
        assertEquals(1, summary.getUnprocessedAlerts().size());
        assertEquals("Test Alert", summary.getUnprocessedAlerts().get(0).getTitle());

        assertNotNull(summary.getCurrentTask());
        assertEquals("Test Task", summary.getCurrentTask().getTaskName());

        // Verify cache set
        verify(valueOperations).set(eq("dashboard:kpi"), anyString(), eq(1L), eq(TimeUnit.MINUTES));
    }
}

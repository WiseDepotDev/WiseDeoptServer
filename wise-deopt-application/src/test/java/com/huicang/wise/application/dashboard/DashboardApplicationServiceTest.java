package com.huicang.wise.application.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.inspection.InspectionTask;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertEventRepository;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceCoreRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionTaskRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.InventoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 首页看板应用服务的单元测试：KPI 缓存命中/未命中两条路径、缓存回写、待处理告警与当前任务。
 *
 * <p>本批钉住一处**注释与行为不符**的缺陷（只记录、未修）：{@code parseKpi} 的注释写着 "缓存不可用时回退到实时统计"，但 {@code getSummary}
 * 只要判定缓存**非空**就走缓存分支 —— 于是**畸形缓存值**（例如 "abc"，split 后不足 4 段）会让四个 KPI 全部为 {@code null}，
 * 并且**完全不会**去查实时数据。
 */
@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    private static final String KPI_KEY = "dashboard:kpi";
    private static final String PROGRESS_KEY = "inspection:progress";

    @Mock private InventoryRepository inventoryRepository;
    @Mock private AlertEventRepository alertEventRepository;
    @Mock private InspectionTaskRepository inspectionTaskRepository;
    @Mock private DeviceCoreRepository deviceCoreRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private DashboardApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new DashboardApplicationService(
                        inventoryRepository,
                        alertEventRepository,
                        inspectionTaskRepository,
                        deviceCoreRepository,
                        stringRedisTemplate);
    }

    private AlertEvent alert(Short level) {
        AlertEvent entity = new AlertEvent();
        entity.setTitle("标题");
        entity.setSourceModule("DEVICE");
        entity.setLevel(level);
        entity.setMessage("消息");
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    private InspectionTask task() {
        InspectionTask entity = new InspectionTask();
        entity.setTaskId(9L);
        entity.setDeviceId(3L);
        entity.setStatus((short) 1);
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    /** 让缓存未命中路径可以走通：进度、告警列表与当前任务都给空值。 */
    private void stubCacheMissBase() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(PROGRESS_KEY)).thenReturn(null);
        when(alertEventRepository.findByStatusOrderByCreateTimeDesc(0)).thenReturn(List.of());
        when(inspectionTaskRepository.findFirstByStatusOrderByCreateTimeDesc((short) 1))
                .thenReturn(Optional.empty());
    }

    // ---------------- 缓存命中 ----------------

    @Test
    @DisplayName("缓存命中：四个 KPI 全部取自缓存，不查任何实时数据")
    void cacheHitUsesCachedKpi() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KPI_KEY)).thenReturn("10|2|30|5");
        when(alertEventRepository.findByStatusOrderByCreateTimeDesc(0)).thenReturn(List.of());
        when(inspectionTaskRepository.findFirstByStatusOrderByCreateTimeDesc((short) 1))
                .thenReturn(Optional.empty());

        DashboardSummaryDTO summary = service.getSummary();

        assertEquals(10L, summary.getInventoryTotal().longValue());
        assertEquals(2L, summary.getTodayAlertCount().longValue());
        assertEquals(30, summary.getInspectionProgress());
        assertEquals(5L, summary.getDeviceOnlineCount().longValue());
        verify(inventoryRepository, never()).sumTotalQuantity();
        verify(alertEventRepository, never()).countByCreateTimeBetween(any(), any());
        verify(deviceCoreRepository, never()).countByStatus(any());
        verify(valueOperations, never()).get(PROGRESS_KEY);
    }

    @Test
    @DisplayName("缓存命中：仍会加载待处理告警与当前任务（这两项不走缓存）")
    void cacheHitStillLoadsAlertsAndTask() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KPI_KEY)).thenReturn("10|2|30|5");
        when(alertEventRepository.findByStatusOrderByCreateTimeDesc(0))
                .thenReturn(List.of(alert((short) 3), alert(null)));
        when(inspectionTaskRepository.findFirstByStatusOrderByCreateTimeDesc((short) 1))
                .thenReturn(Optional.of(task()));

        DashboardSummaryDTO summary = service.getSummary();

        assertEquals(2, summary.getUnprocessedAlerts().size());
        assertEquals(3, summary.getUnprocessedAlerts().get(0).getLevel());
        assertNull(summary.getUnprocessedAlerts().get(1).getLevel());
        assertNotNull(summary.getCurrentTask());
        assertEquals(9L, summary.getCurrentTask().getTaskId().longValue());
    }

    // ---------------- 缓存未命中 ----------------

    @Test
    @DisplayName("缓存未命中：实时统计四个 KPI，并把结果按 管道分隔 回写缓存 1 分钟")
    void cacheMissComputesAndCachesKpi() {
        stubCacheMissBase();
        when(valueOperations.get(KPI_KEY)).thenReturn(null);
        when(inventoryRepository.sumTotalQuantity()).thenReturn(10);
        when(alertEventRepository.countByCreateTimeBetween(any(), any())).thenReturn(2L);
        when(valueOperations.get(PROGRESS_KEY)).thenReturn("3");
        when(deviceCoreRepository.countByStatus((short) 1)).thenReturn(5L);

        DashboardSummaryDTO summary = service.getSummary();

        assertEquals(10L, summary.getInventoryTotal().longValue());
        assertEquals(2L, summary.getTodayAlertCount().longValue());
        assertEquals(3, summary.getInspectionProgress());
        assertEquals(5L, summary.getDeviceOnlineCount().longValue());

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations)
                .set(eq(KPI_KEY), valueCaptor.capture(), eq(1L), eq(TimeUnit.MINUTES));
        assertEquals("10|2|3|5", valueCaptor.getValue());
    }

    @Test
    @DisplayName("缓存未命中：库存求和为 null 时记 0")
    void cacheMissHandlesNullInventorySum() {
        stubCacheMissBase();
        when(valueOperations.get(KPI_KEY)).thenReturn(null);
        when(inventoryRepository.sumTotalQuantity()).thenReturn(null);
        when(alertEventRepository.countByCreateTimeBetween(any(), any())).thenReturn(0L);
        when(deviceCoreRepository.countByStatus((short) 1)).thenReturn(0L);

        assertEquals(0L, service.getSummary().getInventoryTotal().longValue());
    }

    @Test
    @DisplayName("缓存未命中：今日告警窗口是当天 00:00:00 到 23:59:59.999999999")
    void cacheMissUsesTodayWindow() {
        stubCacheMissBase();
        when(valueOperations.get(KPI_KEY)).thenReturn(null);
        when(inventoryRepository.sumTotalQuantity()).thenReturn(1);
        when(alertEventRepository.countByCreateTimeBetween(any(), any())).thenReturn(0L);
        when(deviceCoreRepository.countByStatus((short) 1)).thenReturn(0L);

        service.getSummary();

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(alertEventRepository).countByCreateTimeBetween(from.capture(), to.capture());
        assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.MIN), from.getValue());
        assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.MAX), to.getValue());
    }

    @Test
    @DisplayName("缓存未命中：巡检进度缓存非法时按 0 处理（不抛异常）")
    void cacheMissHandlesInvalidProgress() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KPI_KEY)).thenReturn(null);
        when(inventoryRepository.sumTotalQuantity()).thenReturn(1);
        when(alertEventRepository.countByCreateTimeBetween(any(), any())).thenReturn(0L);
        when(valueOperations.get(PROGRESS_KEY)).thenReturn("不是数字");
        when(deviceCoreRepository.countByStatus((short) 1)).thenReturn(0L);
        when(alertEventRepository.findByStatusOrderByCreateTimeDesc(0)).thenReturn(List.of());
        when(inspectionTaskRepository.findFirstByStatusOrderByCreateTimeDesc((short) 1))
                .thenReturn(Optional.empty());

        assertEquals(0, service.getSummary().getInspectionProgress());
    }

    @Test
    @DisplayName("缓存未命中：进度缓存为空白串时按 0 处理")
    void cacheMissHandlesBlankProgress() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KPI_KEY)).thenReturn(null);
        when(inventoryRepository.sumTotalQuantity()).thenReturn(1);
        when(alertEventRepository.countByCreateTimeBetween(any(), any())).thenReturn(0L);
        when(valueOperations.get(PROGRESS_KEY)).thenReturn("   ");
        when(deviceCoreRepository.countByStatus((short) 1)).thenReturn(0L);
        when(alertEventRepository.findByStatusOrderByCreateTimeDesc(0)).thenReturn(List.of());
        when(inspectionTaskRepository.findFirstByStatusOrderByCreateTimeDesc((short) 1))
                .thenReturn(Optional.empty());

        assertEquals(0, service.getSummary().getInspectionProgress());
    }

    // ---------------- 缺陷固定 ----------------

    @Test
    @DisplayName("现状缺陷：畸形缓存值让 KPI 全为 null，且并不“回退到实时统计”")
    void malformedCacheDoesNotFallBackToRealtime() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KPI_KEY)).thenReturn("abc");
        when(alertEventRepository.findByStatusOrderByCreateTimeDesc(0)).thenReturn(List.of());
        when(inspectionTaskRepository.findFirstByStatusOrderByCreateTimeDesc((short) 1))
                .thenReturn(Optional.empty());

        DashboardSummaryDTO summary = service.getSummary();

        assertNull(summary.getInventoryTotal(), "现状：畸形缓存下库存总量为 null");
        assertNull(summary.getTodayAlertCount(), "现状：畸形缓存下今日告警数为 null");
        assertNull(summary.getInspectionProgress());
        assertNull(summary.getDeviceOnlineCount());
        verify(inventoryRepository, never()).sumTotalQuantity();
        verify(alertEventRepository, never()).countByCreateTimeBetween(any(), any());
    }

    @Test
    @DisplayName("现状：缓存段数不足时静默返回空 KPI（不抛异常、不告警）")
    void shortCacheValueIsSilentlyEmpty() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KPI_KEY)).thenReturn("10|2");
        when(alertEventRepository.findByStatusOrderByCreateTimeDesc(0)).thenReturn(List.of());
        when(inspectionTaskRepository.findFirstByStatusOrderByCreateTimeDesc((short) 1))
                .thenReturn(Optional.empty());

        DashboardSummaryDTO summary = service.getSummary();

        assertNull(summary.getInventoryTotal());
        assertNull(summary.getDeviceOnlineCount());
    }

    @Test
    @DisplayName("缓存命中：段数多于 4 时取前四段")
    void extraCacheSegmentsAreIgnored() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KPI_KEY)).thenReturn("1|2|3|4|5");
        when(alertEventRepository.findByStatusOrderByCreateTimeDesc(0)).thenReturn(List.of());
        when(inspectionTaskRepository.findFirstByStatusOrderByCreateTimeDesc((short) 1))
                .thenReturn(Optional.empty());

        DashboardSummaryDTO summary = service.getSummary();

        assertEquals(1L, summary.getInventoryTotal().longValue());
        assertEquals(4L, summary.getDeviceOnlineCount().longValue());
    }

    // ---------------- 最近任务 ----------------

    @Test
    @DisplayName("最近任务：按 limit 透传并映射")
    void getRecentTasksMapsAll() {
        when(inspectionTaskRepository.findRecentTasks(5)).thenReturn(List.of(task(), task()));

        assertEquals(2, service.getRecentTasks(5).size());

        verify(inspectionTaskRepository).findRecentTasks(5);
    }

    @Test
    @DisplayName("最近任务：无数据返回空表")
    void getRecentTasksEmpty() {
        when(inspectionTaskRepository.findRecentTasks(anyInt())).thenReturn(List.of());

        assertTrue(service.getRecentTasks(0).isEmpty());
    }
}

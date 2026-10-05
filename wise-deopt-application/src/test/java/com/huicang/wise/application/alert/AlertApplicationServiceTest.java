package com.huicang.wise.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.dashboard.DashboardKpiCache;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.alert.AlertHandleLog;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertHandleLogRepository;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserCoreRepository;
import com.huicang.wise.infrastructure.redis.RedisCacheManager;
import com.huicang.wise.infrastructure.redis.RedisCacheUtils;
import com.huicang.wise.infrastructure.redis.RedisKeys;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 告警应用服务的单元测试：创建校验与缓存副作用、分页筛选、确认与状态流转、处理日志、统计口径。
 *
 * <p>本批把两个**现状缺陷**用断言固定下来（修好之后这些断言会失败，属预期）： ① {@code listAlertEvents} 的 {@code level} 与 {@code
 * status} 筛选用的是 {@code Integer.equals(Short)}， 恒为 false ⇒ **一旦传入这两个筛选条件就永远返回空表**； ② 告警级别/处理日志里的
 * {@code levelDescription}、{@code statusDescription}、{@code handlerName}、 {@code
 * goalStatusDescription} 恒被写成空串（占位，从未真正填充）。
 */
@ExtendWith(MockitoExtension.class)
class AlertApplicationServiceTest {

    private static final long EVENT_ID = 88L;

    /**
     * 当前登录用户（测试里固定为 9）。
     *
     * <p>处置记录里的操作人**只能**来自认证上下文 —— 请求体里没有这个字段（防冒名）， 所以每次调用都要显式传进来。
     */
    private static final long OPERATOR_ID = 9L;

    @Mock private AlertRepository alertRepository;
    @Mock private AlertHandleLogRepository alertHandleLogRepository;
    @Mock private UserCoreRepository userCoreRepository;
    @Mock private DashboardKpiCache dashboardKpiCache;
    @Mock private RedisCacheManager cacheManager;

    private AlertApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new AlertApplicationService(
                        alertRepository,
                        alertHandleLogRepository,
                        userCoreRepository,
                        dashboardKpiCache);
    }

    private AlertEvent event(Short status, Short level, String sourceModule, Boolean active) {
        AlertEvent entity = new AlertEvent();
        entity.setStatus(status);
        entity.setLevel(level);
        entity.setSourceModule(sourceModule);
        entity.setIsActive(active);
        entity.setTitle("标题");
        return entity;
    }

    private void stubRedisList() {
        // 缓存门面显式绑定：单测里没有容器，不绑就是 NPE（且会变成"靠别的测试类先跑"的顺序依赖）
        new RedisCacheUtils(cacheManager);
    }

    private AlertCreateRequest createRequest(String level) {
        AlertCreateRequest request = new AlertCreateRequest();
        request.setAlertType("MANUAL");
        request.setAlertLevel(level);
        request.setDescription("温度过高");
        return request;
    }

    // ---------------- 创建 ----------------

    @Test
    @DisplayName("创建告警：类型为空被拒")
    void createAlertRejectsBlankType() {
        AlertCreateRequest request = createRequest("1");
        request.setAlertType("  ");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createAlert(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(alertRepository, never()).save(any(AlertEvent.class));
    }

    @Test
    @DisplayName("创建告警：类型为 null 被拒")
    void createAlertRejectsNullType() {
        AlertCreateRequest request = createRequest("1");
        request.setAlertType(null);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createAlert(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("创建告警：来源缺省为 MANUAL，初始状态 0 且有效")
    void createAlertDefaultsSourceModule() {
        stubRedisList();
        when(alertRepository.save(any(AlertEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(service.createAlert(createRequest("2")));

        ArgumentCaptor<AlertEvent> captor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertRepository).save(captor.capture());
        AlertEvent saved = captor.getValue();
        assertEquals("MANUAL", saved.getSourceModule());
        assertEquals((short) 2, saved.getLevel());
        assertEquals((short) 0, saved.getStatus());
        assertEquals(Boolean.TRUE, saved.getIsActive());
        assertEquals("手动告警", saved.getTitle());
        assertEquals("温度过高", saved.getMessage());
        assertNotNull(saved.getCreateTime());
    }

    @Test
    @DisplayName("创建告警：级别非法 ⇒ PARAM_ERROR（原来是漏 NumberFormatException，被转成 500）")
    void createAlertInvalidLevelIsParameterError() {
        AlertCreateRequest request = createRequest("高");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createAlert(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        assertTrue(
                String.valueOf(ex.getMessage()).contains("级别"),
                "要让调用方知道是哪个参数不对，实得：" + ex.getMessage());
        verify(alertRepository, never()).save(any(AlertEvent.class));
    }

    @Test
    @DisplayName("创建告警：写入未处理列表并清总览缓存")
    void createAlertWritesCache() {
        stubRedisList();
        when(alertRepository.save(any(AlertEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createAlert(createRequest("3"));

        verify(cacheManager).lPush(RedisKeys.ALERT_UNHANDLED_LIST, "null|MANUAL|3");
        verify(cacheManager).expire(RedisKeys.ALERT_UNHANDLED_LIST, 6, TimeUnit.HOURS);
        verify(dashboardKpiCache).invalidate();
    }

    // ---------------- 按级别查询 ----------------

    @Test
    @DisplayName("按级别查询：级别为空或空串时查全部（条件为 null）")
    void listAlertsByLevelWithoutLevel() {
        when(alertRepository.findByLevel(null))
                .thenReturn(List.of(event((short) 0, (short) 1, "X", true)));

        assertEquals(1, service.listAlertsByLevel(null).size());
        assertEquals(1, service.listAlertsByLevel("").size());
    }

    @Test
    @DisplayName("按级别查询：非数字改为抛 PARAM_ERROR（不再静默退化为查全部）")
    void listAlertsByLevelRejectsNonNumeric() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.listAlertsByLevel("abc"));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(alertRepository, never()).findByLevel(any());
    }

    @Test
    @DisplayName("按级别查询：数字级别透传并映射")
    void listAlertsByLevelParsesNumber() {
        when(alertRepository.findByLevel(2))
                .thenReturn(List.of(event((short) 0, (short) 2, "DEVICE", true)));

        List<AlertDTO> rows = service.listAlertsByLevel("2");

        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).getLevel());
        assertEquals("DEVICE", rows.get(0).getSourceModule());
    }

    // ---------------- 分页筛选 ----------------

    @Test
    @DisplayName("告警分页：页码与页大小兜底，且切片正确")
    void listAlertEventsDefaultsAndSlices() {
        List<AlertEvent> all =
                List.of(
                        event((short) 0, (short) 1, "DEVICE", true),
                        event((short) 0, (short) 1, "DEVICE", true),
                        event((short) 0, (short) 1, "DEVICE", true));
        when(alertRepository.findAll()).thenReturn(all);

        AlertEventPageDTO result = service.listAlertEvents(0, 0, null, null, null, null);

        assertEquals(3L, result.getTotal());
        assertEquals(3, result.getRows().size());
    }

    @Test
    @DisplayName("告警分页：页码越界时返回空行但保留 total")
    void listAlertEventsOutOfRangePage() {
        when(alertRepository.findAll())
                .thenReturn(List.of(event((short) 0, (short) 1, "DEVICE", true)));

        AlertEventPageDTO result = service.listAlertEvents(5, 10, null, null, null, null);

        assertEquals(1L, result.getTotal());
        assertEquals(0, result.getRows().size());
    }

    @Test
    @DisplayName("告警分页：level 筛选生效（修复 Integer.equals(Short) 恒 false）")
    void listAlertEventsFiltersByLevel() {
        when(alertRepository.findAll())
                .thenReturn(
                        List.of(
                                event((short) 0, (short) 0, "DEVICE", true),
                                event((short) 0, (short) 2, "DEVICE", true)));

        AlertEventPageDTO result = service.listAlertEvents(1, 10, null, 0, null, null);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRows().size());
        assertEquals(0, result.getRows().get(0).getLevel());
    }

    @Test
    @DisplayName("告警分页：status 筛选生效（修复后不再返回空表）")
    void listAlertEventsFiltersByStatus() {
        when(alertRepository.findAll())
                .thenReturn(
                        List.of(
                                event((short) 0, (short) 1, "DEVICE", true),
                                event((short) 2, (short) 1, "DEVICE", false)));

        AlertEventPageDTO result = service.listAlertEvents(1, 10, null, null, 0, null);

        assertEquals(1L, result.getTotal());
        assertEquals(0, result.getRows().get(0).getStatus());
    }

    @Test
    @DisplayName("告警分页：来源模块筛选生效（空串视为不过滤）")
    void listAlertEventsFiltersBySourceModule() {
        when(alertRepository.findAll())
                .thenReturn(
                        List.of(
                                event((short) 0, (short) 1, "DEVICE", true),
                                event((short) 0, (short) 1, "SYSTEM", true)));

        assertEquals(
                1, service.listAlertEvents(1, 10, "DEVICE", null, null, null).getRows().size());
        assertEquals(2, service.listAlertEvents(1, 10, "  ", null, null, null).getRows().size());
    }

    @Test
    @DisplayName("告警分页：是否有效筛选生效")
    void listAlertEventsFiltersByIsActive() {
        when(alertRepository.findAll())
                .thenReturn(
                        List.of(
                                event((short) 0, (short) 1, "DEVICE", true),
                                event((short) 2, (short) 1, "DEVICE", false)));

        assertEquals(
                1,
                service.listAlertEvents(1, 10, null, null, null, Boolean.FALSE).getRows().size());
    }

    // ---------------- 详情 / 确认 / 状态流转 ----------------

    @Test
    @DisplayName("告警详情：不存在抛 NOT_FOUND")
    void getAlertMissing() {
        when(alertRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getAlert(EVENT_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("告警详情：命中返回完整视图")
    void getAlertFound() {
        when(alertRepository.findById(EVENT_ID))
                .thenReturn(Optional.of(event((short) 0, (short) 3, "DEVICE", true)));

        AlertDTO dto = service.getAlert(EVENT_ID);

        assertEquals(3, dto.getLevel());
        assertEquals(0, dto.getStatus());
        assertEquals(Boolean.TRUE, dto.getIsActive());
    }

    @Test
    @DisplayName("确认告警：不存在抛 NOT_FOUND")
    void acknowledgeMissing() {
        when(alertRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.acknowledgeAlert(EVENT_ID, OPERATOR_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("确认告警：已处理过的直接早退，不落库、不写日志、不清缓存")
    void acknowledgeAlreadyHandledIsNoop() {
        when(alertRepository.findById(EVENT_ID))
                .thenReturn(Optional.of(event((short) 1, (short) 1, "DEVICE", true)));

        service.acknowledgeAlert(EVENT_ID, OPERATOR_ID);

        verify(alertRepository, never()).save(any(AlertEvent.class));
        verify(alertHandleLogRepository, never()).save(any(AlertHandleLog.class));
        verify(dashboardKpiCache, never()).invalidate();
    }

    @Test
    @DisplayName("确认告警：状态置 1 并写一条“快速确认”日志")
    void acknowledgeMovesToStatusOneAndLogs() {
        AlertEvent entity = event((short) 0, (short) 2, "DEVICE", true);
        when(alertRepository.findById(EVENT_ID)).thenReturn(Optional.of(entity));

        service.acknowledgeAlert(EVENT_ID, OPERATOR_ID);

        assertEquals((short) 1, entity.getStatus());
        verify(alertRepository).save(entity);
        ArgumentCaptor<AlertHandleLog> captor = ArgumentCaptor.forClass(AlertHandleLog.class);
        verify(alertHandleLogRepository).save(captor.capture());
        AlertHandleLog log = captor.getValue();
        assertEquals(EVENT_ID, log.getEventId());
        assertEquals(OPERATOR_ID, log.getHandlerId(), "处置记录必须记**真实操作人**，不是占位值 1L");
        assertEquals((short) 1, log.getGoalStatus());
        assertEquals("快速确认", log.getRemark());
        assertNotNull(log.getHandleTime());
        verify(dashboardKpiCache).invalidate();
    }

    @Test
    @DisplayName("确认告警：状态为 null 时按未处理处理")
    void acknowledgeTreatsNullStatusAsUnhandled() {
        AlertEvent entity = event(null, (short) 2, "DEVICE", true);
        when(alertRepository.findById(EVENT_ID)).thenReturn(Optional.of(entity));

        service.acknowledgeAlert(EVENT_ID, OPERATOR_ID);

        assertEquals((short) 1, entity.getStatus());
    }

    @Test
    @DisplayName("更新状态：请求或状态为空被拒")
    void updateAlertStatusRejectsMissingRequest() {
        BusinessException ex1 =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.updateAlertStatus(
                                        EVENT_ID, new UpdateAlertStatusRequest(), OPERATOR_ID));
        assertEquals(ErrorCode.PARAM_ERROR, ex1.getErrorCode());

        BusinessException ex2 =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateAlertStatus(EVENT_ID, null, OPERATOR_ID));
        assertEquals(ErrorCode.PARAM_ERROR, ex2.getErrorCode());
    }

    @Test
    @DisplayName("更新状态：告警不存在抛 NOT_FOUND")
    void updateAlertStatusMissingAlert() {
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest();
        request.setStatus(1);
        when(alertRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateAlertStatus(EVENT_ID, request, OPERATOR_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("更新状态为 2（已处理）：写解决人/解决时间并置为无效")
    void updateAlertStatusToResolved() {
        AlertEvent entity = event((short) 1, (short) 2, "DEVICE", true);
        when(alertRepository.findById(EVENT_ID)).thenReturn(Optional.of(entity));
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest();
        request.setStatus(2);

        request.setRemark("已修复");

        service.updateAlertStatus(EVENT_ID, request, OPERATOR_ID);

        assertEquals((short) 2, entity.getStatus());
        assertEquals(OPERATOR_ID, entity.getResolvedBy(), "解决人来自认证上下文，不是请求体");
        assertNotNull(entity.getResolvedTime());
        assertEquals(Boolean.FALSE, entity.getIsActive());
        ArgumentCaptor<AlertHandleLog> captor = ArgumentCaptor.forClass(AlertHandleLog.class);
        verify(alertHandleLogRepository).save(captor.capture());
        assertEquals((short) 2, captor.getValue().getGoalStatus());
        assertEquals("已修复", captor.getValue().getRemark());
    }

    @Test
    @DisplayName("更新状态为 1（处理中）：不动解决人/解决时间/有效性")
    void updateAlertStatusToHandlingKeepsResolutionFields() {
        AlertEvent entity = event((short) 0, (short) 2, "DEVICE", true);
        when(alertRepository.findById(EVENT_ID)).thenReturn(Optional.of(entity));
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest();
        request.setStatus(1);

        service.updateAlertStatus(EVENT_ID, request, OPERATOR_ID);

        assertEquals((short) 1, entity.getStatus());
        assertNull(entity.getResolvedTime());
        assertNull(entity.getResolvedBy());
        assertEquals(Boolean.TRUE, entity.getIsActive());
    }

    // ---------------- 处理日志 / 统计 ----------------

    @Test
    @DisplayName("处理日志：映射并给出 total；处理人姓名与状态描述已真实填充")
    void listAlertHandleLogsMapsRows() {
        AlertHandleLog log = new AlertHandleLog();
        log.setEventId(EVENT_ID);
        log.setHandlerId(9L);
        log.setGoalStatus((short) 2);
        log.setRemark("已修复");
        when(alertHandleLogRepository.findByEventId(EVENT_ID)).thenReturn(List.of(log));
        UserCore handler = new UserCore();
        handler.setUsername("zhang");
        when(userCoreRepository.findById(9L)).thenReturn(Optional.of(handler));

        AlertHandleLogPageDTO page = service.listAlertHandleLogs(EVENT_ID);

        assertEquals(1L, page.getTotal());
        assertEquals("zhang", page.getRows().get(0).getHandlerName());
        assertEquals("已处理", page.getRows().get(0).getGoalStatusDescription());
        assertEquals(2, page.getRows().get(0).getGoalStatus());
    }

    @Test
    @DisplayName("告警摘要：级别与状态描述已真实映射（不再是空串占位）")
    void summaryDescriptionsAreMapped() {
        when(alertRepository.findAll())
                .thenReturn(List.of(event((short) 0, (short) 3, "DEVICE", true)));

        AlertEventPageDTO page = service.listAlertEvents(1, 10, null, null, null, null);

        assertEquals("紧急", page.getRows().get(0).getLevelDescription());
        assertEquals("未处理", page.getRows().get(0).getStatusDescription());
    }

    @Test
    @DisplayName("告警统计：按状态、级别、来源模块分别计数（待处理=未处理+处理中）")
    void getAlertStatisticsCountsByStatusLevelAndModule() {
        when(alertRepository.findAll())
                .thenReturn(
                        List.of(
                                event((short) 0, (short) 3, "DEVICE", true),
                                event((short) 1, (short) 2, "INVENTORY", true),
                                event((short) 2, (short) 0, "SYSTEM", false),
                                event((short) 3, (short) 1, "RFID_VIDEO", true),
                                event(null, null, null, null)));

        Map<String, Object> statistics = service.getAlertStatistics();

        assertEquals(5L, statistics.get("totalAlerts"));
        assertEquals(1L, statistics.get("unhandledAlerts"));
        assertEquals(1L, statistics.get("handlingAlerts"));
        assertEquals(1L, statistics.get("handledAlerts"));
        assertEquals(1L, statistics.get("ignoredAlerts"));
        // 只统计"未处理 + 处理中"这两条
        assertEquals(1L, statistics.get("criticalAlerts"));
        assertEquals(1L, statistics.get("severeAlerts"));
        assertEquals(0L, statistics.get("warningAlerts"));
        assertEquals(0L, statistics.get("infoAlerts"));
        assertEquals(1L, statistics.get("deviceAlerts"));
        assertEquals(1L, statistics.get("inventoryAlerts"));
        assertEquals(0L, statistics.get("securityAlerts"));
        assertEquals(0L, statistics.get("systemAlerts"));
    }

    @Test
    @DisplayName("处理完成 / 忽略：操作人取不到时给一句能照做的话（不是字段校验错误）")
    void updateAlertStatusWithoutOperatorSaysWhatToDo() {
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest();
        request.setStatus(2);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateAlertStatus(EVENT_ID, request, null));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        assertTrue(
                String.valueOf(ex.getMessage()).contains("重新登录"),
                "要让用户知道下一步做什么，实得：" + ex.getMessage());
        verify(alertHandleLogRepository, never()).save(any(AlertHandleLog.class));
    }

    @Test
    @DisplayName("快速确认：拿不到操作人同样明确拒绝（不许再写占位值）")
    void acknowledgeWithoutOperatorIsRejected() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.acknowledgeAlert(EVENT_ID, null));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(alertRepository, never()).save(any(AlertEvent.class));
        verify(alertHandleLogRepository, never()).save(any(AlertHandleLog.class));
    }

    @Test
    @DisplayName("处理完成：解决人写的是认证上下文里的操作人（请求体已没有该字段，防冒名）")
    void resolvedByComesFromAuthenticatedOperator() {
        AlertEvent entity = event((short) 0, (short) 3, "DEVICE", true);
        when(alertRepository.findById(EVENT_ID)).thenReturn(Optional.of(entity));
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest();
        request.setStatus(2);
        request.setRemark("已更换滤芯");

        service.updateAlertStatus(EVENT_ID, request, OPERATOR_ID);

        assertEquals(OPERATOR_ID, entity.getResolvedBy());
        ArgumentCaptor<AlertHandleLog> captor = ArgumentCaptor.forClass(AlertHandleLog.class);
        verify(alertHandleLogRepository).save(captor.capture());
        assertEquals(OPERATOR_ID, captor.getValue().getHandlerId());
        assertEquals("已更换滤芯", captor.getValue().getRemark());
    }
}

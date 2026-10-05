package com.huicang.wise.application.alert;

import com.huicang.wise.application.dashboard.DashboardKpiCache;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.alert.AlertHandleLog;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertHandleLogRepository;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserCoreRepository;
import com.huicang.wise.infrastructure.redis.RedisCacheUtils;
import com.huicang.wise.infrastructure.redis.RedisKeys;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 类功能描述：告警应用服务
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现告警用例编排
 */
@Service
public class AlertApplicationService {

    private final AlertRepository alertRepository;
    private final AlertHandleLogRepository alertHandleLogRepository;
    private final UserCoreRepository userCoreRepository;
    private final DashboardKpiCache dashboardKpiCache;

    public AlertApplicationService(
            AlertRepository alertRepository,
            AlertHandleLogRepository alertHandleLogRepository,
            UserCoreRepository userCoreRepository,
            DashboardKpiCache dashboardKpiCache) {
        this.alertRepository = alertRepository;
        this.alertHandleLogRepository = alertHandleLogRepository;
        this.userCoreRepository = userCoreRepository;
        this.dashboardKpiCache = dashboardKpiCache;
    }

    /**
     * 方法功能描述：创建告警事件
     *
     * @param request 告警创建请求
     * @return 告警信息
     * @throws BusinessException 当告警类型为空时抛出异常
     */
    @Transactional
    public AlertDTO createAlert(AlertCreateRequest request) throws BusinessException {
        if (request.getAlertType() == null || request.getAlertType().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "告警类型不能为空");
        }
        AlertEvent entity = new AlertEvent();
        entity.setSourceModule(
                request.getSourceModule() != null ? request.getSourceModule() : "MANUAL");
        entity.setLevel(parseLevel(request.getAlertLevel()));
        entity.setTitle("手动告警");
        entity.setMessage(request.getDescription());
        entity.setStatus((short) 0);
        entity.setIsActive(true);
        entity.setCreateTime(LocalDateTime.now());
        AlertEvent saved = alertRepository.save(entity);
        cacheUnhandledAlert(saved);

        // Clear dashboard KPI cache to ensure data overview updates
        dashboardKpiCache.invalidate();

        return toAlertDTO(saved);
    }

    /**
     * 告警级别：必须能解析成数字。
     *
     * <p>原来直接 `Short.parseShort` —— 传 `"WARNING"` 这种人类可读写法时抛 `NumberFormatException`，被全局处理器转成
     * `SYS-0001 系统异常`（500）。 而这是**输入不合法**，接口自己的说明写的是"参数错误返回400"： 500 会让调用方以为服务坏了、也会把参数问题埋进日志里。
     */
    private Short parseLevel(String alertLevel) {
        try {
            return Short.parseShort(alertLevel);
        } catch (NumberFormatException | NullPointerException e) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR, "告警级别必须是数字（0 提示 / 1 警告 / 2 严重 / 3 紧急）");
        }
    }

    /**
     * 方法功能描述：按告警级别查询告警列表
     *
     * @param alertLevel 告警级别
     * @return 告警列表
     */
    public List<AlertDTO> listAlertsByLevel(String alertLevel) {
        Integer level = null;
        if (alertLevel != null && !alertLevel.isEmpty()) {
            try {
                level = Integer.parseInt(alertLevel);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "告警级别格式不正确: " + alertLevel);
            }
        }
        List<AlertEvent> entities = alertRepository.findByLevel(level);
        return entities.stream().map(this::toAlertDTO).collect(Collectors.toList());
    }

    public AlertEventPageDTO listAlertEvents(
            Integer page,
            Integer size,
            String sourceModule,
            Integer level,
            Integer status,
            Boolean isActive) {
        int pageIndex = page == null || page < 1 ? 0 : page - 1;
        int pageSize = size == null || size < 1 ? 10 : size;
        List<AlertEvent> filtered =
                alertRepository.findAll().stream()
                        .filter(
                                entity ->
                                        sourceModule == null
                                                || sourceModule.isBlank()
                                                || sourceModule.equals(entity.getSourceModule()))
                        .filter(entity -> matchesNumber(level, entity.getLevel()))
                        .filter(entity -> matchesNumber(status, entity.getStatus()))
                        .filter(entity -> isActive == null || isActive.equals(entity.getIsActive()))
                        .collect(Collectors.toList());
        int total = filtered.size();
        int fromIndex = pageIndex * pageSize;
        if (fromIndex >= total) {
            AlertEventPageDTO empty = new AlertEventPageDTO();
            empty.setTotal((long) total);
            empty.setRows(Collections.emptyList());
            return empty;
        }
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<AlertEventSummaryDTO> rows =
                filtered.subList(fromIndex, toIndex).stream()
                        .map(this::toAlertEventSummaryDTO)
                        .collect(Collectors.toList());
        AlertEventPageDTO dto = new AlertEventPageDTO();
        dto.setTotal((long) total);
        dto.setRows(rows);
        return dto;
    }

    /**
     * 方法功能描述：查询告警详情
     *
     * @param eventId 告警事件ID
     * @return 告警信息
     * @throws BusinessException 当告警不存在时抛出异常
     */
    public AlertDTO getAlert(Long eventId) throws BusinessException {
        AlertEvent entity =
                alertRepository
                        .findById(eventId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "告警不存在"));
        return toAlertDTO(entity);
    }

    /**
     * 快速确认（ACK）。
     *
     * @param operatorId 操作人 —— **由服务端从认证上下文取**，不由请求体提供。 这里原来是硬编码的
     *     `setHandlerId(1L)`：每一次"快速确认"都把处置记录记到 1 号用户头上 （一个谁都不知道是谁的占位值），比"没记"更糟 —— 它看起来像有据可查。
     */
    @Transactional
    public void acknowledgeAlert(Long eventId, Long operatorId) throws BusinessException {
        Long operator = requireOperator(operatorId);
        AlertEvent entity =
                alertRepository
                        .findById(eventId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "告警不存在"));

        // 如果已经是处理状态，则无需重复ACK
        if (entity.getStatus() != null && entity.getStatus() != 0) {
            return;
        }

        entity.setStatus((short) 1); // 1: Acknowledged
        // ACK 只表示"有人看到了、正在处理"，不代表已解决，所以不动 isActive。
        alertRepository.save(entity);

        AlertHandleLog log = new AlertHandleLog();
        log.setEventId(eventId);
        log.setHandlerId(operator);
        log.setGoalStatus((short) 1);
        log.setRemark("快速确认");
        log.setHandleTime(LocalDateTime.now());
        alertHandleLogRepository.save(log);

        // Clear dashboard KPI cache to ensure data overview updates
        dashboardKpiCache.invalidate();
    }

    /**
     * 更新告警状态（处理中 / 处理完成 / 忽略）。
     *
     * @param operatorId 操作人 —— 同上，来自认证上下文；**请求体里没有这个字段** （原来有，且客户端拿不到，于是"处理完成/忽略"必然在写库时被 `@NotNull`
     *     拦下）。
     */
    @Transactional
    public void updateAlertStatus(Long eventId, UpdateAlertStatusRequest request, Long operatorId)
            throws BusinessException {
        if (request == null || request.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "告警状态不能为空");
        }
        Long operator = requireOperator(operatorId);
        AlertEvent entity =
                alertRepository
                        .findById(eventId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "告警不存在"));
        Integer status = request.getStatus();
        entity.setStatus(status.shortValue());
        if (status == 2) {
            entity.setResolvedBy(operator);
            entity.setResolvedTime(LocalDateTime.now());
            entity.setIsActive(false);
        }
        alertRepository.save(entity);

        AlertHandleLog log = new AlertHandleLog();
        log.setEventId(eventId);
        log.setHandlerId(operator);
        log.setGoalStatus(status.shortValue());
        log.setRemark(request.getRemark());
        log.setHandleTime(LocalDateTime.now());
        alertHandleLogRepository.save(log);
    }

    /**
     * 操作人**必须**能确定：拿不到就明确说"重新登录"，而不是让它变成一句看不懂的字段校验错误。
     *
     * <p>正常路径上它来自 Bearer 令牌（`JwtAuthenticationFilter` 把 userId 放进 request attribute）； 为空只有一种可能 ——
     * 这条请求没有经过认证（例如签名链路），那属于调用方用错了接口。
     */
    private Long requireOperator(Long operatorId) {
        if (operatorId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无法确定操作人：请重新登录后再试一次");
        }
        return operatorId;
    }

    public AlertHandleLogPageDTO listAlertHandleLogs(Long eventId) {
        List<AlertHandleLogDTO> rows =
                alertHandleLogRepository.findByEventId(eventId).stream()
                        .map(this::toAlertHandleLogDTO)
                        .collect(Collectors.toList());
        AlertHandleLogPageDTO dto = new AlertHandleLogPageDTO();
        dto.setTotal((long) rows.size());
        dto.setRows(rows);
        return dto;
    }

    /** 按数值比较可空枚举：{@code Integer.equals(Short)} 恒为 false，曾把筛选变成"清空开关"。 */
    private static boolean matchesNumber(Integer expected, Number actual) {
        return expected == null || (actual != null && expected.intValue() == actual.intValue());
    }

    private static String levelDescription(Short level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case 0:
                return "提示";
            case 1:
                return "警告";
            case 2:
                return "严重";
            case 3:
                return "紧急";
            default:
                return "未知";
        }
    }

    private static String statusDescription(Short status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case 0:
                return "未处理";
            case 1:
                return "处理中";
            case 2:
                return "已处理";
            case 3:
                return "已忽略";
            default:
                return "未知";
        }
    }

    private String resolveHandlerName(Long handlerId) {
        if (handlerId == null) {
            return null;
        }
        return userCoreRepository.findById(handlerId).map(UserCore::getUsername).orElse(null);
    }

    private AlertDTO toAlertDTO(AlertEvent entity) {
        AlertDTO dto = new AlertDTO();
        dto.setEventId(entity.getEventId());
        dto.setTitle(entity.getTitle());
        dto.setSourceModule(entity.getSourceModule());
        dto.setLevel(entity.getLevel() != null ? entity.getLevel().intValue() : null);
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().intValue() : null);
        dto.setIsActive(entity.getIsActive());
        dto.setCreateTime(entity.getCreateTime());
        dto.setResolvedTime(entity.getResolvedTime());
        dto.setResolvedBy(entity.getResolvedBy());
        dto.setExtendedData(entity.getExtendedData());
        return dto;
    }

    private AlertEventSummaryDTO toAlertEventSummaryDTO(AlertEvent entity) {
        AlertEventSummaryDTO dto = new AlertEventSummaryDTO();
        dto.setEventId(entity.getEventId());
        dto.setSourceModule(entity.getSourceModule());
        dto.setLevel(entity.getLevel() != null ? entity.getLevel().intValue() : null);
        dto.setLevelDescription(levelDescription(entity.getLevel()));
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().intValue() : null);
        dto.setStatusDescription(statusDescription(entity.getStatus()));
        dto.setIsActive(entity.getIsActive());
        dto.setCreateTime(entity.getCreateTime());
        dto.setResolvedTime(entity.getResolvedTime());
        dto.setResolvedBy(entity.getResolvedBy());
        dto.setExtendedData(entity.getExtendedData());
        return dto;
    }

    private AlertHandleLogDTO toAlertHandleLogDTO(AlertHandleLog entity) {
        AlertHandleLogDTO dto = new AlertHandleLogDTO();
        dto.setLogId(entity.getLogId());
        dto.setEventId(entity.getEventId());
        dto.setHandlerId(entity.getHandlerId());
        dto.setHandlerName(resolveHandlerName(entity.getHandlerId()));
        dto.setGoalStatus(
                entity.getGoalStatus() != null ? entity.getGoalStatus().intValue() : null);
        dto.setGoalStatusDescription(statusDescription(entity.getGoalStatus()));
        dto.setRemark(entity.getRemark());
        dto.setHandleTime(entity.getHandleTime());
        return dto;
    }

    private void cacheUnhandledAlert(AlertEvent entity) {
        String key = RedisKeys.ALERT_UNHANDLED_LIST;
        String value =
                entity.getEventId() + "|" + entity.getSourceModule() + "|" + entity.getLevel();
        RedisCacheUtils.lPush(key, value);
        RedisCacheUtils.expire(key, 6, TimeUnit.HOURS);
    }

    /**
     * 方法功能描述：获取告警统计信息
     *
     * @return 告警统计信息
     */
    public Map<String, Object> getAlertStatistics() {
        List<AlertEvent> allAlerts = alertRepository.findAll();

        Map<String, Object> statistics = new HashMap<>();

        long totalAlerts = allAlerts.size();
        long unhandledAlerts =
                allAlerts.stream().filter(a -> a.getStatus() != null && a.getStatus() == 0).count();
        long handlingAlerts =
                allAlerts.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();
        long handledAlerts =
                allAlerts.stream().filter(a -> a.getStatus() != null && a.getStatus() == 2).count();
        long ignoredAlerts =
                allAlerts.stream().filter(a -> a.getStatus() != null && a.getStatus() == 3).count();

        // 统计待处理告警（未处理 + 处理中），排除已处理和已忽略的告警
        List<AlertEvent> pendingAlerts =
                allAlerts.stream()
                        .filter(
                                a ->
                                        a.getStatus() != null
                                                && (a.getStatus() == 0 || a.getStatus() == 1))
                        .collect(Collectors.toList());

        long criticalAlerts =
                pendingAlerts.stream()
                        .filter(a -> a.getLevel() != null && a.getLevel() == 3)
                        .count();
        long severeAlerts =
                pendingAlerts.stream()
                        .filter(a -> a.getLevel() != null && a.getLevel() == 2)
                        .count();
        long warningAlerts =
                pendingAlerts.stream()
                        .filter(a -> a.getLevel() != null && a.getLevel() == 1)
                        .count();
        long infoAlerts =
                pendingAlerts.stream()
                        .filter(a -> a.getLevel() != null && a.getLevel() == 0)
                        .count();

        long deviceAlerts =
                pendingAlerts.stream()
                        .filter(
                                a ->
                                        a.getSourceModule() != null
                                                && a.getSourceModule().equals("DEVICE"))
                        .count();
        long inventoryAlerts =
                pendingAlerts.stream()
                        .filter(
                                a ->
                                        a.getSourceModule() != null
                                                && a.getSourceModule().equals("INVENTORY"))
                        .count();
        long securityAlerts =
                pendingAlerts.stream()
                        .filter(
                                a ->
                                        a.getSourceModule() != null
                                                && a.getSourceModule().equals("RFID_VIDEO"))
                        .count();
        long systemAlerts =
                pendingAlerts.stream()
                        .filter(
                                a ->
                                        a.getSourceModule() != null
                                                && a.getSourceModule().equals("SYSTEM"))
                        .count();

        statistics.put("totalAlerts", totalAlerts);
        statistics.put("unhandledAlerts", unhandledAlerts);
        statistics.put("handlingAlerts", handlingAlerts);
        statistics.put("handledAlerts", handledAlerts);
        statistics.put("ignoredAlerts", ignoredAlerts);

        statistics.put("criticalAlerts", criticalAlerts);
        statistics.put("severeAlerts", severeAlerts);
        statistics.put("warningAlerts", warningAlerts);
        statistics.put("infoAlerts", infoAlerts);

        statistics.put("deviceAlerts", deviceAlerts);
        statistics.put("inventoryAlerts", inventoryAlerts);
        statistics.put("securityAlerts", securityAlerts);
        statistics.put("systemAlerts", systemAlerts);

        return statistics;
    }
}

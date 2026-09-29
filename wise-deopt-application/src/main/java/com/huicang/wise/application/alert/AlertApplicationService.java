package com.huicang.wise.application.alert;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.alert.AlertHandleLog;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertHandleLogRepository;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserCoreRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate stringRedisTemplate;

    public AlertApplicationService(
            AlertRepository alertRepository,
            AlertHandleLogRepository alertHandleLogRepository,
            UserCoreRepository userCoreRepository,
            StringRedisTemplate stringRedisTemplate) {
        this.alertRepository = alertRepository;
        this.alertHandleLogRepository = alertHandleLogRepository;
        this.userCoreRepository = userCoreRepository;
        this.stringRedisTemplate = stringRedisTemplate;
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
        entity.setLevel(Short.parseShort(request.getAlertLevel()));
        entity.setTitle("手动告警");
        entity.setMessage(request.getDescription());
        entity.setStatus((short) 0);
        entity.setIsActive(true);
        entity.setCreateTime(LocalDateTime.now());
        AlertEvent saved = alertRepository.save(entity);
        cacheUnhandledAlert(saved);

        // Clear dashboard KPI cache to ensure data overview updates
        stringRedisTemplate.delete("dashboard:kpi");

        return toAlertDTO(saved);
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

    @Transactional
    public void acknowledgeAlert(Long eventId) throws BusinessException {
        AlertEvent entity =
                alertRepository
                        .findById(eventId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "告警不存在"));

        // 如果已经是处理状态，则无需重复ACK
        if (entity.getStatus() != null && entity.getStatus() != 0) {
            return;
        }

        entity.setStatus((short) 1); // 1: Acknowledged
        // ACK usually means someone is looking at it, but it's not necessarily resolved.
        // However, based on existing logic, non-zero status sets isActive=false.
        // We might want to keep it active or follow existing logic.
        // Let's assume ACK means "handled" in the sense of "checked".
        // If we want to keep it active, we should change the logic in updateAlertStatus or here.
        // For now, let's follow updateAlertStatus logic style:
        // ACK implies it is still an issue, just known.

        alertRepository.save(entity);

        AlertHandleLog log = new AlertHandleLog();
        log.setEventId(eventId);
        log.setHandlerId(1L);
        log.setGoalStatus((short) 1);
        log.setRemark("快速确认");
        log.setHandleTime(LocalDateTime.now());
        alertHandleLogRepository.save(log);

        // Clear dashboard KPI cache to ensure data overview updates
        stringRedisTemplate.delete("dashboard:kpi");
    }

    @Transactional
    public void updateAlertStatus(Long eventId, UpdateAlertStatusRequest request)
            throws BusinessException {
        if (request == null || request.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "告警状态不能为空");
        }
        AlertEvent entity =
                alertRepository
                        .findById(eventId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "告警不存在"));
        Integer status = request.getStatus();
        entity.setStatus(status.shortValue());
        if (status == 2) {
            entity.setResolvedBy(request.getHandlerId());
            entity.setResolvedTime(LocalDateTime.now());
            entity.setIsActive(false);
        }
        alertRepository.save(entity);

        AlertHandleLog log = new AlertHandleLog();
        log.setEventId(eventId);
        log.setHandlerId(request.getHandlerId());
        log.setGoalStatus(status.shortValue());
        log.setRemark(request.getRemark());
        log.setHandleTime(LocalDateTime.now());
        alertHandleLogRepository.save(log);
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
        String key = "alert:unhandled:list";
        String value =
                entity.getEventId() + "|" + entity.getSourceModule() + "|" + entity.getLevel();
        stringRedisTemplate.opsForList().leftPush(key, value);
        stringRedisTemplate.expire(key, Duration.ofHours(6));
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

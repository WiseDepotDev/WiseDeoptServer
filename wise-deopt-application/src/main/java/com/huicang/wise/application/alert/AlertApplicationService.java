package com.huicang.wise.application.alert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.alert.AlertEventJpaEntity;
import com.huicang.wise.infrastructure.repository.alert.AlertEventRepository;
import com.huicang.wise.infrastructure.repository.alert.AlertHandleLogJpaEntity;
import com.huicang.wise.infrastructure.repository.alert.AlertHandleLogRepository;

/**
 * 类功能描述：告警应用服务
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现告警用例编排
 */
@Service
public class AlertApplicationService {

    private final AlertEventRepository alertEventRepository;
    private final AlertHandleLogRepository alertHandleLogRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public AlertApplicationService(AlertEventRepository alertEventRepository,
                                   AlertHandleLogRepository alertHandleLogRepository,
                                   StringRedisTemplate stringRedisTemplate) {
        this.alertEventRepository = alertEventRepository;
        this.alertHandleLogRepository = alertHandleLogRepository;
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
        AlertEventJpaEntity entity = new AlertEventJpaEntity();
        entity.setEventId(System.currentTimeMillis());
        entity.setAlertType(request.getAlertType());
        entity.setAlertLevel(request.getAlertLevel());
        entity.setDescription(request.getDescription());
        entity.setAlertTime(LocalDateTime.now());
        AlertEventJpaEntity saved = alertEventRepository.save(entity);
        cacheUnhandledAlert(saved);
        return toAlertDTO(saved);
    }

    /**
     * 方法功能描述：按告警级别查询告警列表
     *
     * @param alertLevel 告警级别
     * @return 告警列表
     */
    public List<AlertDTO> listAlertsByLevel(String alertLevel) {
        List<AlertEventJpaEntity> entities = alertEventRepository.findByAlertLevel(alertLevel);
        return entities.stream().map(this::toAlertDTO).collect(Collectors.toList());
    }

    public AlertEventPageDTO listAlertEvents(Integer page,
                                             Integer size,
                                             String sourceModule,
                                             Integer level,
                                             Integer status,
                                             Boolean isActive) {
        int pageIndex = page == null || page < 1 ? 0 : page - 1;
        int pageSize = size == null || size < 1 ? 10 : size;
        List<AlertEventJpaEntity> filtered = alertEventRepository.findAll().stream()
                .filter(entity -> sourceModule == null || sourceModule.isBlank()
                        || sourceModule.equals(entity.getSourceModule()))
                .filter(entity -> level == null || level.equals(entity.getLevel()))
                .filter(entity -> status == null || status.equals(entity.getStatus()))
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
        List<AlertEventSummaryDTO> rows = filtered.subList(fromIndex, toIndex).stream()
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
        AlertEventJpaEntity entity = alertEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "告警不存在"));
        return toAlertDTO(entity);
    }

    @Transactional
    public void updateAlertStatus(Long eventId, UpdateAlertStatusRequest request) throws BusinessException {
        if (request == null || request.getStatus() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "告警状态不能为空");
        }
        AlertEventJpaEntity entity = alertEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "告警不存在"));
        Integer status = request.getStatus();
        entity.setStatus(status);
        if (status == 0) {
            entity.setIsActive(true);
            entity.setResolvedTime(null);
        } else {
            entity.setIsActive(false);
            entity.setResolvedTime(LocalDateTime.now());
        }
        alertEventRepository.save(entity);

        AlertHandleLogJpaEntity log = new AlertHandleLogJpaEntity();
        log.setLogId(System.currentTimeMillis());
        log.setEventId(eventId);
        log.setGoalStatus(status);
        log.setGoalStatusDescription("");
        log.setRemark(request.getRemark());
        log.setHandleTime(LocalDateTime.now());
        alertHandleLogRepository.save(log);
    }

    public AlertHandleLogPageDTO listAlertHandleLogs(Long eventId) {
        List<AlertHandleLogDTO> rows = alertHandleLogRepository.findByEventId(eventId).stream()
                .map(this::toAlertHandleLogDTO)
                .collect(Collectors.toList());
        AlertHandleLogPageDTO dto = new AlertHandleLogPageDTO();
        dto.setTotal((long) rows.size());
        dto.setRows(rows);
        return dto;
    }

    private AlertDTO toAlertDTO(AlertEventJpaEntity entity) {
        AlertDTO dto = new AlertDTO();
        dto.setEventId(entity.getEventId());
        dto.setAlertType(entity.getAlertType());
        dto.setAlertLevel(entity.getAlertLevel());
        dto.setDescription(entity.getDescription());
        dto.setAlertTime(entity.getAlertTime());
        return dto;
    }

    private AlertEventSummaryDTO toAlertEventSummaryDTO(AlertEventJpaEntity entity) {
        AlertEventSummaryDTO dto = new AlertEventSummaryDTO();
        dto.setEventId(entity.getEventId());
        dto.setSourceModule(entity.getSourceModule());
        dto.setLevel(entity.getLevel());
        dto.setLevelDescription("");
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setStatus(entity.getStatus());
        dto.setStatusDescription("");
        dto.setIsActive(entity.getIsActive());
        dto.setCreateTime(entity.getAlertTime());
        dto.setResolvedTime(entity.getResolvedTime());
        dto.setResolvedBy(entity.getResolvedBy());
        dto.setExtendedData(entity.getExtendedData());
        return dto;
    }

    private AlertHandleLogDTO toAlertHandleLogDTO(AlertHandleLogJpaEntity entity) {
        AlertHandleLogDTO dto = new AlertHandleLogDTO();
        dto.setLogId(entity.getLogId());
        dto.setEventId(entity.getEventId());
        dto.setHandlerId(entity.getHandlerId());
        dto.setHandlerName(entity.getHandlerName());
        dto.setGoalStatus(entity.getGoalStatus());
        dto.setGoalStatusDescription(entity.getGoalStatusDescription());
        dto.setRemark(entity.getRemark());
        dto.setHandleTime(entity.getHandleTime());
        return dto;
    }

    private void cacheUnhandledAlert(AlertEventJpaEntity entity) {
        String key = "alert:unhandled:list";
        String value = entity.getEventId() + "|" + entity.getAlertType() + "|" + entity.getAlertLevel();
        stringRedisTemplate.opsForList().leftPush(key, value);
        stringRedisTemplate.expire(key, Duration.ofHours(6));
    }
}

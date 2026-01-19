package com.huicang.wise.application.alert;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.alert.AlertEventJpaEntity;
import com.huicang.wise.infrastructure.repository.alert.AlertEventRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private final StringRedisTemplate stringRedisTemplate;

    public AlertApplicationService(AlertEventRepository alertEventRepository,
                                   StringRedisTemplate stringRedisTemplate) {
        this.alertEventRepository = alertEventRepository;
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

    private AlertDTO toAlertDTO(AlertEventJpaEntity entity) {
        AlertDTO dto = new AlertDTO();
        dto.setEventId(entity.getEventId());
        dto.setAlertType(entity.getAlertType());
        dto.setAlertLevel(entity.getAlertLevel());
        dto.setDescription(entity.getDescription());
        dto.setAlertTime(entity.getAlertTime());
        return dto;
    }

    private void cacheUnhandledAlert(AlertEventJpaEntity entity) {
        String key = "alert:unhandled:list";
        String value = entity.getEventId() + "|" + entity.getAlertType() + "|" + entity.getAlertLevel();
        stringRedisTemplate.opsForList().leftPush(key, value);
        stringRedisTemplate.expire(key, Duration.ofHours(6));
    }
}


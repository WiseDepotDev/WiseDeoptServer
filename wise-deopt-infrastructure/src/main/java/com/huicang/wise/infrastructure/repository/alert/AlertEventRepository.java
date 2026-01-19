package com.huicang.wise.infrastructure.repository.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 类功能描述：告警事件仓储接口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义告警查询接口
 */
public interface AlertEventRepository extends JpaRepository<AlertEventJpaEntity, Long> {

    /**
     * 方法功能描述：按告警级别查询告警
     *
     * @param alertLevel 告警级别
     * @return 告警事件列表
     */
    List<AlertEventJpaEntity> findByAlertLevel(String alertLevel);

    long countByAlertTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}

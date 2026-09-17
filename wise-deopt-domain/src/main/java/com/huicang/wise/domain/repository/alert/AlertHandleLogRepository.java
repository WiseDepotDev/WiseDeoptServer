package com.huicang.wise.domain.repository.alert;

import com.huicang.wise.domain.alert.AlertHandleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警处理日志仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface AlertHandleLogRepository extends JpaRepository<AlertHandleLog, Long> {

    /**
     * 根据告警事件ID查询处理日志
     *
     * @param eventId 告警事件ID
     * @return 处理日志列表
     */
    List<AlertHandleLog> findByEventId(Long eventId);
}
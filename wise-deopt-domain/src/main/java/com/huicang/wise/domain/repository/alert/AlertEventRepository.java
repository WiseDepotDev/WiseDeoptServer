package com.huicang.wise.domain.repository.alert;

import com.huicang.wise.domain.alert.AlertEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警事件仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    /**
     * 根据告警ID查询告警事件
     *
     * @param eventId 告警ID
     * @return 告警事件
     */
    Optional<AlertEvent> findByEventId(Long eventId);

    /**
     * 根据告警级别查询告警事件列表
     *
     * @param level 告警级别
     * @return 告警事件列表
     */
    List<AlertEvent> findByLevel(Integer level);

    /**
     * 根据状态查询告警事件列表
     *
     * @param status 状态
     * @return 告警事件列表
     */
    List<AlertEvent> findByStatus(Integer status);

    /**
     * 根据创建时间范围查询告警事件列表
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 告警事件列表
     */
    List<AlertEvent> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询今日告警数量
     *
     * @param startTime 今日开始时间
     * @param endTime   今日结束时间
     * @return 告警数量
     */
    long countByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询未处理的告警事件列表
     *
     * @param status 处理状态
     * @param pageable 分页参数
     * @return 告警事件分页结果
     */
    Page<AlertEvent> findByStatusOrderByCreateTimeDesc(Integer status, Pageable pageable);

    /**
     * 查询未处理的告警事件列表（不分页）
     *
     * @param status 处理状态
     * @return 告警事件列表
     */
    List<AlertEvent> findByStatusOrderByCreateTimeDesc(Integer status);

    /**
     * 根据告警ID删除告警事件
     *
     * @param eventId 告警ID
     */
    void deleteByEventId(Long eventId);

    /**
     * 查询包含指定RFID的未解决告警
     *
     * @param rfid RFID标签
     * @param status 告警状态
     * @return 告警事件列表
     */
    @Query("SELECT a FROM AlertEvent a WHERE a.status = :status AND a.message LIKE %:rfid%")
    List<AlertEvent> findByStatusAndMessageContainingRfid(@Param("status") Integer status, @Param("rfid") String rfid);
}

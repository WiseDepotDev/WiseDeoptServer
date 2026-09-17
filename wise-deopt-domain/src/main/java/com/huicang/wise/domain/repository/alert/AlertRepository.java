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
 * 告警仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface AlertRepository extends JpaRepository<AlertEvent, Long> {

    /**
     * 根据告警ID查询告警事件
     *
     * @param eventId 告警ID
     * @return 告警事件
     */
    Optional<AlertEvent> findByEventId(Long eventId);

    /**
     * 根据告警级别查询告警事件
     *
     * @param level 告警级别
     * @return 告警事件列表
     */
    List<AlertEvent> findByLevel(Integer level);

    /**
     * 根据告警级别分页查询告警事件
     *
     * @param level 告警级别
     * @param pageable   分页参数
     * @return 告警事件分页结果
     */
    Page<AlertEvent> findByLevel(Integer level, Pageable pageable);

    /**
     * 根据告警状态查询告警事件
     *
     * @param status 告警状态
     * @return 告警事件列表
     */
    List<AlertEvent> findByStatus(Integer status);

    /**
     * 根据告警状态分页查询告警事件
     *
     * @param status 告警状态
     * @param pageable    分页参数
     * @return 告警事件分页结果
     */
    Page<AlertEvent> findByStatus(Integer status, Pageable pageable);

    /**
     * 分页查询告警事件列表
     *
     * @param pageable 分页参数
     * @return 告警事件分页结果
     */
    Page<AlertEvent> findAll(Pageable pageable);

    /**
     * 根据时间范围查询告警事件
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 告警事件列表
     */
    List<AlertEvent> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间范围分页查询告警事件
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 告警事件分页结果
     */
    Page<AlertEvent> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据告警级别和状态查询告警事件
     *
     * @param level   告警级别
     * @param status 告警状态
     * @return 告警事件列表
     */
    List<AlertEvent> findByLevelAndStatus(Integer level, Integer status);

    /**
     * 根据告警级别和状态分页查询告警事件
     *
     * @param level   告警级别
     * @param status 告警状态
     * @param pageable    分页参数
     * @return 告警事件分页结果
     */
    Page<AlertEvent> findByLevelAndStatus(Integer level, Integer status, Pageable pageable);

    /**
     * 根据告警级别统计告警数量
     *
     * @param level 告警级别
     * @return 告警数量
     */
    long countByLevel(Integer level);

    /**
     * 根据告警状态统计告警数量
     *
     * @param status 告警状态
     * @return 告警数量
     */
    long countByStatus(Integer status);

    /**
     * 查询未处理的告警
     *
     * @param status 告警状态
     * @return 未处理告警列表
     */
    @Query("SELECT a FROM AlertEvent a WHERE a.status = :status")
    List<AlertEvent> findUnhandledAlerts(@Param("status") Integer status);

    /**
     * 查询紧急告警
     *
     * @param level 告警级别
     * @param status 告警状态
     * @return 紧急告警列表
     */
    @Query("SELECT a FROM AlertEvent a WHERE a.level = :level AND a.status = :status")
    List<AlertEvent> findUrgentAlerts(@Param("level") Integer level, @Param("status") Integer status);
}

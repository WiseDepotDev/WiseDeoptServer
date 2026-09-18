package com.huicang.wise.infrastructure.persistence.repository.inspection;

import com.huicang.wise.domain.inspection.InspectionPlan;
import com.huicang.wise.domain.inspection.InspectionTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 巡检仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface InspectionRepository extends JpaRepository<InspectionPlan, Long> {

    /**
     * 根据计划名称模糊查询巡检计划
     *
     * @param planName 计划名称（支持模糊匹配）
     * @param pageable 分页参数
     * @return 巡检计划分页结果
     */
    @Query("SELECT p FROM InspectionPlan p WHERE p.planName LIKE %:planName%")
    Page<InspectionPlan> findByPlanNameContaining(
            @Param("planName") String planName, Pageable pageable);

    /**
     * 根据计划状态查询巡检计划
     *
     * @param status 计划状态
     * @return 巡检计划列表
     */
    List<InspectionPlan> findByStatus(Short status);

    /**
     * 根据计划状态分页查询巡检计划
     *
     * @param status 计划状态
     * @param pageable 分页参数
     * @return 巡检计划分页结果
     */
    Page<InspectionPlan> findByStatus(Short status, Pageable pageable);

    /**
     * 分页查询巡检计划列表
     *
     * @param pageable 分页参数
     * @return 巡检计划分页结果
     */
    Page<InspectionPlan> findAll(Pageable pageable);

    /**
     * 根据任务ID查询巡检任务
     *
     * @param taskId 任务ID
     * @return 巡检任务
     */
    @Query("SELECT t FROM InspectionTask t WHERE t.taskId = :taskId")
    Optional<InspectionTask> findTaskById(@Param("taskId") Long taskId);

    /**
     * 根据计划ID查询巡检任务列表
     *
     * @param planId 计划ID
     * @return 巡检任务列表
     */
    @Query("SELECT t FROM InspectionTask t WHERE t.planId = :planId")
    List<InspectionTask> findTasksByPlanId(@Param("planId") Long planId);

    /**
     * 根据任务状态查询巡检任务
     *
     * @param status 任务状态
     * @return 巡检任务列表
     */
    @Query("SELECT t FROM InspectionTask t WHERE t.status = :status")
    List<InspectionTask> findTasksByStatus(@Param("status") Short status);

    /**
     * 根据计划状态统计计划数量
     *
     * @param status 计划状态
     * @return 计划数量
     */
    long countByStatus(Short status);

    /**
     * 根据任务状态统计任务数量
     *
     * @param status 任务状态
     * @return 任务数量
     */
    @Query(
            value = "SELECT COUNT(*) FROM inspection_task WHERE status = :status",
            nativeQuery = true)
    long countTasksByStatus(@Param("status") Short status);

    /**
     * 查询待执行的巡检任务
     *
     * @param status 任务状态
     * @param currentTime 当前时间
     * @return 待执行任务列表
     */
    @Query(
            "SELECT t FROM InspectionTask t WHERE t.status = :status AND t.startTime <= :currentTime")
    List<InspectionTask> findPendingTasks(
            @Param("status") Short status,
            @Param("currentTime") java.time.LocalDateTime currentTime);
}

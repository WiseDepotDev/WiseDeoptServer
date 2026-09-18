package com.huicang.wise.domain.repository.inspection;

import com.huicang.wise.domain.inspection.InspectionTask;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionTaskRepository extends JpaRepository<InspectionTask, Long> {

    List<InspectionTask> findByPlanId(Long planId);

    List<InspectionTask> findByStatus(Short status);

    @Query(
            "SELECT t FROM InspectionTask t WHERE "
                    + "(:planId IS NULL OR t.planId = :planId) AND "
                    + "(:warehouseId IS NULL OR t.warehouseId = :warehouseId) AND "
                    + "(:taskType IS NULL OR t.taskType = :taskType) AND "
                    + "(:status IS NULL OR t.status = :status) AND "
                    + "(:deviceId IS NULL OR t.deviceId = :deviceId)")
    List<InspectionTask> findByConditions(
            @Param("planId") Long planId,
            @Param("warehouseId") Long warehouseId,
            @Param("taskType") Short taskType,
            @Param("status") Short status,
            @Param("deviceId") Long deviceId);

    @Query(
            "SELECT t FROM InspectionTask t WHERE "
                    + "(:planId IS NULL OR t.planId = :planId) AND "
                    + "(:warehouseId IS NULL OR t.warehouseId = :warehouseId) AND "
                    + "(:taskType IS NULL OR t.taskType = :taskType) AND "
                    + "(:status IS NULL OR t.status = :status) AND "
                    + "(:deviceId IS NULL OR t.deviceId = :deviceId)")
    Page<InspectionTask> findByConditions(
            @Param("planId") Long planId,
            @Param("warehouseId") Long warehouseId,
            @Param("taskType") Short taskType,
            @Param("status") Short status,
            @Param("deviceId") Long deviceId,
            Pageable pageable);

    List<InspectionTask> findByDeviceIdAndStatus(Long deviceId, Short status);

    @Query(
            value = "SELECT COUNT(*) FROM inspection_task WHERE status = :status",
            nativeQuery = true)
    Long countByStatus(@Param("status") Short status);

    @Query(
            "SELECT t FROM InspectionTask t WHERE t.status = :status "
                    + "AND t.createTime <= :currentTime")
    List<InspectionTask> findPendingTasks(
            @Param("status") Short status, @Param("currentTime") LocalDateTime currentTime);

    @Query(
            value = "SELECT * FROM inspection_task ORDER BY create_time DESC LIMIT :limit",
            nativeQuery = true)
    List<InspectionTask> findRecentTasks(@Param("limit") int limit);

    Optional<InspectionTask> findFirstByStatusOrderByCreateTimeDesc(Short status);
}

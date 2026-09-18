package com.huicang.wise.domain.repository.inspection;

import com.huicang.wise.domain.inspection.InspectionPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionPlanRepository extends JpaRepository<InspectionPlan, Long> {
    Optional<InspectionPlan> findByPlanName(String planName);

    @Query("SELECT p FROM InspectionPlan p WHERE " + "(:status IS NULL OR p.status = :status)")
    List<InspectionPlan> findByConditions(@Param("status") Short status);

    @Query("SELECT p FROM InspectionPlan p WHERE p.status = :status")
    List<InspectionPlan> findPendingPlans(@Param("status") Short status);

    @Query(
            value = "SELECT COUNT(*) FROM inspection_plan WHERE status = :status",
            nativeQuery = true)
    Long countEnabledPlans(@Param("status") Short status);
}

package com.huicang.wise.infrastructure.repository.inspection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 巡检任务仓储
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Repository
public interface InspectionTaskRepository extends JpaRepository<InspectionTaskJpaEntity, Long> {
    List<InspectionTaskJpaEntity> findByPlanId(Long planId);
    List<InspectionTaskJpaEntity> findByDeviceId(Long deviceId);
    List<InspectionTaskJpaEntity> findByStatus(Integer status);
    List<InspectionTaskJpaEntity> findByDeviceIdAndStatus(Long deviceId, Integer status);
}

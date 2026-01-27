package com.huicang.wise.infrastructure.repository.inspection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 巡检明细仓储
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Repository
public interface InspectionDetailRepository extends JpaRepository<InspectionDetailJpaEntity, Long> {
    List<InspectionDetailJpaEntity> findByTaskId(Long taskId);
}

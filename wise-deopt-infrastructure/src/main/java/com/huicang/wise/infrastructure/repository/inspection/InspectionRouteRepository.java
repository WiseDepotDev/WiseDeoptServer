package com.huicang.wise.infrastructure.repository.inspection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 巡检路线仓储
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Repository
public interface InspectionRouteRepository extends JpaRepository<InspectionRouteJpaEntity, Long> {
}

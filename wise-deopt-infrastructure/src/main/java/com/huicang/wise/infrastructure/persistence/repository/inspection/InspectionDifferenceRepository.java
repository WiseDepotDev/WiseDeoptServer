package com.huicang.wise.infrastructure.persistence.repository.inspection;

import com.huicang.wise.domain.inspection.InspectionDifference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionDifferenceRepository extends JpaRepository<InspectionDifference, Long> {
    List<InspectionDifference> findByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);
}

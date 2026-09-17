package com.huicang.wise.domain.repository.inspection;

import com.huicang.wise.domain.inspection.InspectionDifference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionDifferenceRepository extends JpaRepository<InspectionDifference, Long> {
    List<InspectionDifference> findByTaskId(Long taskId);
    void deleteByTaskId(Long taskId);
}

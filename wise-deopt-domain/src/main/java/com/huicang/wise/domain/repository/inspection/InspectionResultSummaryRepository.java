package com.huicang.wise.domain.repository.inspection;

import com.huicang.wise.domain.inspection.InspectionResultSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionResultSummaryRepository extends JpaRepository<InspectionResultSummary, Long> {

    List<InspectionResultSummary> findByTaskId(Long taskId);
}

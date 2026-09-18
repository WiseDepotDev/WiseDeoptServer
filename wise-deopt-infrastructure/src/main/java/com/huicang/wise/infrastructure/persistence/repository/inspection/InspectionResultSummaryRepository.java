package com.huicang.wise.infrastructure.persistence.repository.inspection;

import com.huicang.wise.domain.inspection.InspectionResultSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionResultSummaryRepository
        extends JpaRepository<InspectionResultSummary, Long> {

    List<InspectionResultSummary> findByTaskId(Long taskId);
}

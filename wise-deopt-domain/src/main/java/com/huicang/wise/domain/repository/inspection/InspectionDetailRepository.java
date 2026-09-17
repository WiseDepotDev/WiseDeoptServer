package com.huicang.wise.domain.repository.inspection;

import com.huicang.wise.domain.inspection.InspectionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionDetailRepository extends JpaRepository<InspectionDetail, Long> {
    @Query("SELECT d FROM InspectionDetail d WHERE d.matched = :matched")
    List<InspectionDetail> findByMatched(@Param("matched") Short matched);

    @Query("SELECT d FROM InspectionDetail d WHERE " +
           "(:taskId IS NULL OR d.taskId = :taskId) AND " +
           "(:matched IS NULL OR d.matched = :matched)")
    List<InspectionDetail> findByConditions(
        @Param("taskId") Long taskId,
        @Param("matched") Short matched
    );

    @Query(value = "SELECT COUNT(*) FROM inspection_detail WHERE matched = :matched", nativeQuery = true)
    Long countByMatched(@Param("matched") Short matched);

    @Query(value = "SELECT matched, COUNT(*) FROM inspection_detail GROUP BY matched", nativeQuery = true)
    List<Object[]> countByMatchedGroup();

    List<InspectionDetail> findByTaskId(Long taskId);
}
package com.huicang.wise.infrastructure.repository.inspection;

import lombok.Data;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 巡检结果实体
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Entity
@Table(name = "inspection_result")
public class InspectionResultJpaEntity {

    @Id
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "total_expected")
    private Integer totalExpected;

    @Column(name = "total_scanned")
    private Integer totalScanned;

    @Column(name = "matched_count")
    private Integer matchedCount;

    @Column(name = "missing_count")
    private Integer missingCount;

    @Column(name = "extra_count")
    private Integer extraCount;

    @Column(name = "compare_time")
    private LocalDateTime compareTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "missing_products")
    private String missingProducts; // JSON

    @Column(name = "extra_products")
    private String extraProducts; // JSON
}

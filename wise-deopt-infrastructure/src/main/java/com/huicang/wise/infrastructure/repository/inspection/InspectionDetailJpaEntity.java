package com.huicang.wise.infrastructure.repository.inspection;

import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 巡检明细实体
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Entity
@Table(name = "inspection_detail")
public class InspectionDetailJpaEntity {

    @Id
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "rfid")
    private String rfid;

    @Column(name = "scan_time")
    private LocalDateTime scanTime;

    /**
     * 是否匹配：0-不匹配，1-匹配
     */
    @Column(name = "matched")
    private Integer matched;

    @Column(name = "remark")
    private String remark;
}

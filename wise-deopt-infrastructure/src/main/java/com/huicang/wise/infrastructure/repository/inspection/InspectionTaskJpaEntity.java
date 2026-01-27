package com.huicang.wise.infrastructure.repository.inspection;

import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 巡检任务实体
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Entity
@Table(name = "inspection_task")
public class InspectionTaskJpaEntity {

    @Id
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "plan_id")
    private Long planId;

    /**
     * 任务类型：0-定时任务，1-手动任务
     */
    @Column(name = "task_type")
    private Integer taskType;

    @Column(name = "device_id")
    private Long deviceId;

    /**
     * 状态：0-待执行，1-执行中，2-已完成，3-异常终止
     */
    @Column(name = "status")
    private Integer status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}

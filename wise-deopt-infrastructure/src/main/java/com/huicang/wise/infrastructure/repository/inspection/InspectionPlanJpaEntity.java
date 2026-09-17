package com.huicang.wise.infrastructure.repository.inspection;

import lombok.Data;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 巡检计划实体
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Entity
@Table(name = "inspection_plan")
public class InspectionPlanJpaEntity {

    @Id
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "cron_expression")
    private String cronExpression;

    /**
     * 状态：0-禁用，1-启用
     */
    @Column(name = "status", columnDefinition = "tinyint unsigned")
    private Short status;

    @Column(name = "last_execute_time")
    private LocalDateTime lastExecuteTime;

    @Column(name = "nett_execute_time")
    private LocalDateTime nettExecuteTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "update_by")
    private Long updateBy;
}

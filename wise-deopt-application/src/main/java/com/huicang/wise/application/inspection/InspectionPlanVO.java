package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡检计划VO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Schema(description = "巡检计划VO")
public class InspectionPlanVO {

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "定时表达式")
    private String cronExpression;

    @Schema(description = "状态：0-禁用 1-启用")
    private Short status;

    @Schema(description = "状态描述")
    private String statusDescription;

    @Schema(description = "上次执行时间")
    private LocalDateTime lastExecuteTime;

    @Schema(description = "下次执行时间")
    private LocalDateTime nettExecuteTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建人名称")
    private String createByName;
}


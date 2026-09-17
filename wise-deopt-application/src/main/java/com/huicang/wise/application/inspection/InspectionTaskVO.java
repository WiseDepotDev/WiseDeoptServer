package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡检任务VO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Schema(description = "巡检任务VO")
public class InspectionTaskVO {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "任务类型：0-定时 1-手动")
    private Integer taskType;

    @Schema(description = "任务类型描述")
    private String taskTypeDescription;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "状态：0-待执行 1-执行中 2-已完成 3-异常")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDescription;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}


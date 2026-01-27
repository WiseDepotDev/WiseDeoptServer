package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description = "巡检任务VO")
public class InspectionTaskVO {

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("计划ID")
    private Long planId;

    @ApiModelProperty("计划名称")
    private String planName;

    @ApiModelProperty("任务类型：0-定时 1-手动")
    private Integer taskType;

    @ApiModelProperty("任务类型描述")
    private String taskTypeDescription;

    @ApiModelProperty("设备ID")
    private Long deviceId;

    @ApiModelProperty("设备名称")
    private String deviceName;

    @ApiModelProperty("状态：0-待执行 1-执行中 2-已完成 3-异常")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDescription;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}

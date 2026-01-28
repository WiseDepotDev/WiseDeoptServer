package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description = "巡检计划VO")
public class InspectionPlanVO {

    @ApiModelProperty("计划ID")
    private Long planId;

    @ApiModelProperty("计划名称")
    private String planName;

    @ApiModelProperty("设备ID")
    private Long deviceId;

    @ApiModelProperty("设备名称")
    private String deviceName;

    @ApiModelProperty("定时表达式")
    private String cronExpression;

    @ApiModelProperty("巡检路线数据")
    private String routeData;

    @ApiModelProperty("状态：0-禁用 1-启用")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDescription;

    @ApiModelProperty("上次执行时间")
    private LocalDateTime lastExecuteTime;

    @ApiModelProperty("下次执行时间")
    private LocalDateTime nextExecuteTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("创建人ID")
    private Long createBy;

    @ApiModelProperty("创建人名称")
    private String createByName;
}

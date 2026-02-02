package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 更新巡检计划DTO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@ApiModel(description = "更新巡检计划DTO")
public class UpdateInspectionPlanDTO {

    @ApiModelProperty(value = "计划ID", required = true)
    private Long planId;

    @ApiModelProperty(value = "计划名称")
    private String planName;

    @ApiModelProperty(value = "执行巡检设备id")
    private Long deviceId;

    @ApiModelProperty(value = "定时表达式")
    private String cronExpression;

    @ApiModelProperty(value = "巡检路线ID")
    private Long routeId;

    @ApiModelProperty(value = "巡检路线数据")
    private String routeData;


    @ApiModelProperty(value = "状态：0-禁用 1-启用")
    private Integer status;
}

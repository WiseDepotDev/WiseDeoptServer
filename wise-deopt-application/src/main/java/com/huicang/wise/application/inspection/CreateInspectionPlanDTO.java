package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 创建巡检计划DTO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@ApiModel(description = "创建巡检计划DTO")
public class CreateInspectionPlanDTO {

    @ApiModelProperty(value = "计划名称", required = true)
    private String planName;

    @ApiModelProperty(value = "执行巡检设备id", required = true)
    private Long deviceId;

    @ApiModelProperty(value = "定时表达式", required = true)
    private String cronExpression;

    @ApiModelProperty(value = "巡检路线数据")
    private String routeData;
}

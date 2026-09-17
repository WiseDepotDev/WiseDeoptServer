package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新巡检计划DTO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Schema(description = "更新巡检计划DTO")
public class UpdateInspectionPlanDTO {

    @Schema(description = "计划ID", required = true)
    private Long planId;

    @Schema(description = "计划名称")
    private String planName;

    @Schema(description = "执行巡检设备id")
    private Long deviceId;

    @Schema(description = "定时表达式")
    private String cronExpression;

    @Schema(description = "巡检路线ID")
    private Long routeId;

    @Schema(description = "巡检路线数据")
    private String routeData;


    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;
}


package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建巡检计划DTO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Schema(description = "创建巡检计划DTO")
public class CreateInspectionPlanDTO {

    @Schema(description = "计划名称", required = true)
    private String planName;

    @Schema(description = "执行巡检设备id", required = true)
    private Long deviceId;

    @Schema(description = "定时表达式", required = true)
    private String cronExpression;

    @Schema(description = "巡检路线ID")
    private Long routeId;

    @Schema(description = "巡检路线数据（若指定routeId则优先使用路线中的数据）")
    private String routeData;
}


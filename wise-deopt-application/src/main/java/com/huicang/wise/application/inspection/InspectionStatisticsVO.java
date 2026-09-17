package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "巡检统计VO")
public class InspectionStatisticsVO {
    @Schema(description = "今日任务数")
    private Integer todayTaskCount;

    @Schema(description = "今日完成数")
    private Integer todayCompletedCount;

    @Schema(description = "异常任务数")
    private Integer abnormalTaskCount;
    
    @Schema(description = "最近一次任务缺失数")
    private Integer lastMissingCount;

    @Schema(description = "最近一次任务多余数")
    private Integer lastExtraCount;
}


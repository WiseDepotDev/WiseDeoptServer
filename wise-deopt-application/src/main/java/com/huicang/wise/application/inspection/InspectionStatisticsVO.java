package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "巡检统计VO")
public class InspectionStatisticsVO {
    @ApiModelProperty("今日任务数")
    private Integer todayTaskCount;

    @ApiModelProperty("今日完成数")
    private Integer todayCompletedCount;

    @ApiModelProperty("异常任务数")
    private Integer abnormalTaskCount;
    
    @ApiModelProperty("最近一次任务缺失数")
    private Integer lastMissingCount;

    @ApiModelProperty("最近一次任务多余数")
    private Integer lastExtraCount;
}

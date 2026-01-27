package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡检结果摘要VO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@ApiModel(description = "巡检结果摘要VO")
public class InspectionResultSummaryVO {

    @ApiModelProperty("结果ID")
    private Long resultId;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("应扫总数")
    private Integer totalExpected;

    @ApiModelProperty("实扫总数")
    private Integer totalScanned;

    @ApiModelProperty("匹配数")
    private Integer matchedCount;

    @ApiModelProperty("缺失数")
    private Integer missingCount;

    @ApiModelProperty("多余数")
    private Integer extraCount;

    @ApiModelProperty("比对时间")
    private LocalDateTime compareTime;

    @ApiModelProperty("缺失货品详情JSON")
    private String missingProducts;

    @ApiModelProperty("多余货品详情JSON")
    private String extraProducts;
}

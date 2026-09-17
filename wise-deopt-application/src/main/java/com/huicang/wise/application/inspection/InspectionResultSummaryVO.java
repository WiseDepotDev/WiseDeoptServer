package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "巡检结果摘要VO")
public class InspectionResultSummaryVO {

    @Schema(description = "结果ID")
    private Long resultId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "应扫总数")
    private Integer totalExpected;

    @Schema(description = "实扫总数")
    private Integer totalScanned;

    @Schema(description = "匹配数")
    private Integer matchedCount;

    @Schema(description = "缺失数")
    private Integer missingCount;

    @Schema(description = "多余数")
    private Integer extraCount;

    @Schema(description = "比对时间")
    private LocalDateTime compareTime;

    @Schema(description = "缺失货品详情JSON")
    private String missingProducts;

    @Schema(description = "多余货品详情JSON")
    private String extraProducts;
}


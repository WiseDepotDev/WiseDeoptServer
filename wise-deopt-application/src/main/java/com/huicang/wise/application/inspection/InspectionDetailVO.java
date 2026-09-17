package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡检明细VO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Schema(description = "巡检明细VO")
public class InspectionDetailVO {

    @Schema(description = "明细ID")
    private Long detailId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "RFID")
    private String rfid;

    @Schema(description = "扫描时间")
    private LocalDateTime scanTime;

    @Schema(description = "是否匹配：0-不匹配 1-匹配")
    private Short matched;

    @Schema(description = "备注")
    private String remark;
}


package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡检明细DTO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Schema(description = "巡检明细DTO")
public class InspectionDetailDTO {

    @Schema(description = "任务ID", required = true)
    private Long taskId;

    @Schema(description = "RFID", required = true)
    private String rfid;

    @Schema(description = "扫描时间")
    private LocalDateTime scanTime;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "TID")
    private String tid;

    @Schema(description = "备注")
    private String remark;
}


package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description = "巡检明细DTO")
public class InspectionDetailDTO {

    @ApiModelProperty(value = "任务ID", required = true)
    private Long taskId;

    @ApiModelProperty(value = "RFID", required = true)
    private String rfid;

    @ApiModelProperty(value = "扫描时间")
    private LocalDateTime scanTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}

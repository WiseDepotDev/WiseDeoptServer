package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description = "巡检明细VO")
public class InspectionDetailVO {

    @ApiModelProperty("明细ID")
    private Long detailId;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("标签ID")
    private Long tagId;

    @ApiModelProperty("RFID")
    private String rfid;

    @ApiModelProperty("扫描时间")
    private LocalDateTime scanTime;

    @ApiModelProperty("是否匹配：0-不匹配 1-匹配")
    private Integer matched;

    @ApiModelProperty("备注")
    private String remark;
}

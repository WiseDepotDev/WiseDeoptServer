package com.huicang.wise.application.report;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账报表DTO
 *
 * @author xingchentye
 * @date 2026-01-26
 */
@Data
@ApiModel(description = "对账报表DTO")
public class ReconciliationReportDTO {

    @ApiModelProperty("差异ID")
    private Long diffId;

    @ApiModelProperty("产品ID")
    private Long productId;

    @ApiModelProperty("产品名称")
    private String productName;

    @ApiModelProperty("库位编码")
    private String locationCode;

    @ApiModelProperty("预期数量")
    private Integer expectedQuantity;

    @ApiModelProperty("实际数量")
    private Integer actualQuantity;

    @ApiModelProperty("差异类型")
    private String diffType;

    @ApiModelProperty("状态：0-待处理 1-已处理")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}

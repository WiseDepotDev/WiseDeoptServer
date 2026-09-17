package com.huicang.wise.application.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账报表DTO
 *
 * @author xingchentye
 * @date 2026-01-26
 */
@Data
@Schema(description = "对账报表DTO")
public class ReconciliationReportDTO {

    @Schema(description = "差异ID")
    private Long diffId;

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "库位编码")
    private String locationCode;

    @Schema(description = "预期数量")
    private Integer expectedQuantity;

    @Schema(description = "实际数量")
    private Integer actualQuantity;

    @Schema(description = "差异类型")
    private String diffType;

    @Schema(description = "状态：0-待处理 1-已处理")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}


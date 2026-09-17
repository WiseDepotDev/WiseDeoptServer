package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "巡检差异详情VO")
public class InspectionDifferenceVO {

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "预期数量")
    private Integer expectedQuantity;

    @Schema(description = "实际扫描数量")
    private Integer scannedQuantity;

    @Schema(description = "差异数量 (实际 - 预期)")
    private Integer difference;

    @Schema(description = "状态 (NORMAL/MISSING/EXTRA)")
    private String status;
}

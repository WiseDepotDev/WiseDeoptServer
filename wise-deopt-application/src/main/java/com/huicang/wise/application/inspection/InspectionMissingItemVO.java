package com.huicang.wise.application.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "巡检缺失项VO")
public class InspectionMissingItemVO {
    @Schema(description = "产品ID")
    private Long productId;
    
    @Schema(description = "产品名称")
    private String productName;
    
    @Schema(description = "期望位置")
    private String expectedLocation;
    
    @Schema(description = "RFID")
    private String rfid;
}


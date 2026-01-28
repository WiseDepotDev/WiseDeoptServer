package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "巡检缺失项VO")
public class InspectionMissingItemVO {
    @ApiModelProperty("产品ID")
    private Long productId;
    
    @ApiModelProperty("产品名称")
    private String productName;
    
    @ApiModelProperty("期望位置")
    private String expectedLocation;
    
    @ApiModelProperty("RFID")
    private String rfid;
}

package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 创建巡检路线请求
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@ApiModel(description = "创建巡检路线请求")
public class CreateInspectionRouteRequest {

    @ApiModelProperty(value = "路线名称", required = true)
    private String routeName;

    @ApiModelProperty(value = "路线数据（JSON格式）", required = true)
    private String routeData;
}

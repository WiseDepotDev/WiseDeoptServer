package com.huicang.wise.application.inspection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡检路线DTO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@ApiModel(description = "巡检路线DTO")
public class InspectionRouteDTO {

    @ApiModelProperty(value = "路线ID")
    private Long routeId;

    @ApiModelProperty(value = "路线名称")
    private String routeName;

    @ApiModelProperty(value = "路线数据（JSON格式，包含关键点坐标或指令）")
    private String routeData;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}

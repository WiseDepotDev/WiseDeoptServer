package com.huicang.wise.application.rfid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * RFID数据上报DTO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@ApiModel(description = "RFID数据上报DTO")
public class RfidReportDTO {

    /**
     * 设备ID
     */
    @ApiModelProperty(value = "设备ID", required = true)
    private Long deviceId;

    /**
     * RFID标签数组
     */
    @ApiModelProperty(value = "RFID标签数组", required = true)
    private List<String> rfidTags;
}

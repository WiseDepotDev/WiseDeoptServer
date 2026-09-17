package com.huicang.wise.application.rfid;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * RFID数据上报DTO
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 */
@Data
@Schema(description = "RFID数据上报DTO")
public class RfidReportDTO {

    /**
     * 设备ID
     */
    @Schema(description = "设备ID", required = true)
    private Long deviceId;

    /**
     * RFID标签数组
     */
    @Schema(description = "RFID标签数组", required = true)
    private List<String> rfidTags;

    /**
     * 抓拍图片URL
     */
    @Schema(description = "抓拍图片URL", required = false)
    private String snapshotUrl;

    /**
     * 事件时间戳
     */
    @Schema(description = "事件时间戳", required = false)
    private Long eventTime;
}


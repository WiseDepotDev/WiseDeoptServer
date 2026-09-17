package com.huicang.wise.application.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类功能描述：设备心跳请求
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-03-13
 */
@Data
@Schema(description = "设备心跳请求")
public class DeviceHeartbeatRequest {

    @Schema(description = "设备编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceCode;
}

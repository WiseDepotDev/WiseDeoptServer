package com.huicang.wise.domain.device;

import java.util.Map;

/**
 * 设备配置发布接口
 *
 * @author WiseDepot
 * @version 1.0
 */
public interface DeviceConfigPublisher {

    /**
     * 发布配置更新给设备
     *
     * @param deviceCode 设备编码
     * @param config 配置Map
     */
    void publishConfig(String deviceCode, Map<String, Object> config);
}

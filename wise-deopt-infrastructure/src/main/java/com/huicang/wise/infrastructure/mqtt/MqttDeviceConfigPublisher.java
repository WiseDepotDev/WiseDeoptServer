package com.huicang.wise.infrastructure.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.domain.device.DeviceConfigPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MQTT设备配置发布实现
 *
 * @author WiseDepot
 * @version 1.0
 */
@Component
public class MqttDeviceConfigPublisher implements DeviceConfigPublisher {

    private static final Logger logger = LoggerFactory.getLogger(MqttDeviceConfigPublisher.class);

    @Autowired
    private MqttGateway mqttGateway;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void publishConfig(String deviceCode, Map<String, Object> config) {
        try {
            String topic = "wise/device/" + deviceCode + "/config";
            String payload = objectMapper.writeValueAsString(config);
            mqttGateway.sendToMqtt(payload, topic);
            logger.info("已向设备 {} 发布配置更新: {}", deviceCode, payload);
        } catch (JsonProcessingException e) {
            logger.error("序列化设备配置失败", e);
        } catch (Exception e) {
            logger.error("向MQTT发布配置更新失败", e);
        }
    }
}

package com.huicang.wise.infrastructure.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.domain.inspection.TaskMessage;
import com.huicang.wise.domain.inspection.TaskPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MqttTaskPublisher implements TaskPublisher {

    private static final Logger logger = LoggerFactory.getLogger(MqttTaskPublisher.class);

    @Autowired
    private MqttGateway mqttGateway;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void publishTask(String deviceCode, TaskMessage task) {
        try {
            String topic = "wise/device/" + deviceCode + "/task";
            String payload = objectMapper.writeValueAsString(task);
            mqttGateway.sendToMqtt(payload, topic);
            logger.info("已向设备 {} 发布巡检任务: {}", deviceCode, payload);
        } catch (JsonProcessingException e) {
            logger.error("序列化巡检任务失败", e);
        } catch (Exception e) {
            logger.error("向MQTT发布任务失败", e);
        }
    }
}

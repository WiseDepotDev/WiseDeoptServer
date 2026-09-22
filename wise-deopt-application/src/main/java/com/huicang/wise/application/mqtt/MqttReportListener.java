package com.huicang.wise.application.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.application.inspection.InspectionApplicationService;
import com.huicang.wise.application.inspection.InspectionReportRequest;
import com.huicang.wise.application.inspection.InspectionResultDTO;
import com.huicang.wise.common.protocol.EnvelopeUnwrapper;
import com.huicang.wise.domain.inspection.InspectionProgressEvent;
import com.huicang.wise.domain.inspection.InspectionProgressPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class MqttReportListener {

    private static final Logger logger = LoggerFactory.getLogger(MqttReportListener.class);

    @Autowired private InspectionApplicationService inspectionApplicationService;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private InspectionProgressPublisher progressPublisher;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleInspectionReport(Message<String> message) {
        try {
            String raw = message.getPayload();
            String topic = message.getHeaders().get("mqtt_topic", String.class);

            logger.info("Received inspection report from MQTT topic: {}", topic);
            logger.debug("Report payload: {}", raw);

            // 决策 8A（2026-02-27）：MQTT 也支持统一信封——有 header+payload 就取 payload.data，
            // 否则按旧扁平报文处理并打 deprecated=true（与 HTTP 侧 GlobalRequestAdvice 的过渡期做法一致）。
            EnvelopeUnwrapper.Unwrapped unwrapped = EnvelopeUnwrapper.unwrap(raw, objectMapper);
            if (unwrapped.legacy()) {
                logger.warn(
                        "收到无信封 MQTT 报文，已按兼容模式处理: deprecated=true, topic={}, bodyLength={}",
                        topic,
                        raw == null ? 0 : raw.length());
            }

            InspectionReportRequest request =
                    objectMapper.readValue(unwrapped.json(), InspectionReportRequest.class);

            logger.info("Processing inspection report for task: {}", request.getTaskId());

            InspectionResultDTO result = inspectionApplicationService.reportResult(request);

            logger.info(
                    "Inspection report processed successfully for task: {}", request.getTaskId());

            // Publish progress event
            Long taskId = null;
            try {
                if (request.getTaskId() != null && !request.getTaskId().isEmpty()) {
                    taskId = Long.parseLong(request.getTaskId());
                }
            } catch (NumberFormatException e) {
                // ignore
            }

            if (taskId != null) {
                InspectionProgressEvent event =
                        new InspectionProgressEvent(
                                taskId,
                                result.getProgress(),
                                result.getStatus(),
                                result.getTotalScanned(),
                                result.getTotalExpected());
                progressPublisher.publishProgress(event);
                logger.info(
                        "Progress event published for task {}: progress={}, status={}",
                        taskId,
                        result.getProgress(),
                        result.getStatus());
            }

        } catch (Exception e) {
            logger.error("Failed to process inspection report from MQTT", e);
        }
    }
}

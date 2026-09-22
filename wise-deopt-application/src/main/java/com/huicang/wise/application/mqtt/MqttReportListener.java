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

            // 决策 3（2026-02-27）：三端只支持统一信封——无信封报文直接拒绝并记录（不再走兼容分支）。
            EnvelopeUnwrapper.Unwrapped unwrapped = EnvelopeUnwrapper.unwrap(raw, objectMapper);
            if (unwrapped.legacy()) {
                logger.warn(
                        "拒绝无信封 MQTT 报文（需 {header, payload} 结构，STD-CONTRACT-01）: topic={}, bodyLength={}",
                        topic,
                        raw == null ? 0 : raw.length());
                return;
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

package com.huicang.wise.api.websocket;

import com.huicang.wise.domain.inspection.InspectionProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InspectionProgressEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InspectionProgressEventListener.class);

    @EventListener
    public void handleProgressEvent(InspectionProgressEvent event) {
        try {
            Map<String, Object> wsData = new HashMap<>();
            wsData.put("taskId", event.getTaskId());
            wsData.put("progress", event.getProgress());
            wsData.put("status", event.getStatus());
            wsData.put("totalScanned", event.getTotalScanned());
            wsData.put("totalExpected", event.getTotalExpected());
            
            InspectionProgressWebSocket.broadcastProgress(event.getTaskId().toString(), wsData);
            logger.info("Progress broadcasted for task {}: progress={}, status={}", 
                event.getTaskId(), event.getProgress(), event.getStatus());
        } catch (Exception e) {
            logger.error("Failed to broadcast progress event for task: {}", event.getTaskId(), e);
        }
    }
}

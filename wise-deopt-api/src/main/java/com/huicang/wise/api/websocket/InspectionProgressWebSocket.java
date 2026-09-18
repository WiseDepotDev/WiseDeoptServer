package com.huicang.wise.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ServerEndpoint("/ws/inspection/{taskId}")
public class InspectionProgressWebSocket {

    // Store sessions by taskId
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Session>>
            sessionPools = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("taskId") String taskId) {
        sessionPools
                .computeIfAbsent(taskId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        log.info("WebSocket connected for task {}, session id: {}", taskId, session.getId());
    }

    @OnClose
    public void onClose(Session session, @PathParam("taskId") String taskId) {
        ConcurrentHashMap<String, Session> pools = sessionPools.get(taskId);
        if (pools != null) {
            pools.remove(session.getId());
        }
        log.info("WebSocket disconnected for task {}, session id: {}", taskId, session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("taskId") String taskId) {
        // App might send messages, we just echo or ignore for now
        log.debug("Received WebSocket message from {}: {}", session.getId(), message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error", error);
    }

    public static void broadcastProgress(String taskId, Object data) {
        ConcurrentHashMap<String, Session> pools = sessionPools.get(taskId);
        if (pools != null && !pools.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String message = mapper.writeValueAsString(data);
                for (Session session : pools.values()) {
                    if (session.isOpen()) {
                        session.getAsyncRemote().sendText(message);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to broadcast WebSocket progress", e);
            }
        }
    }
}

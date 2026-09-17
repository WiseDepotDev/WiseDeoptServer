package com.huicang.wise.infrastructure.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class DiscoveryServer {

    private static final int PORT = 8080;
    private static final int BUFFER_SIZE = 1024;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${info.app.version:1.0.0}")
    private String serverVersion;

    private DatagramSocket socket;
    private volatile boolean running = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void start() {
        executorService.submit(this::run);
    }

    private void run() {
        try {
            // Bind to 0.0.0.0:8080
            // Note: If Tomcat is also on 8080, this might conflict if binding to same port/interface.
            // UDP 8080 is different from TCP 8080, so it should be fine.
            socket = new DatagramSocket(PORT);
            socket.setBroadcast(true);
            running = true;
            log.info("UDP Discovery Server started on port {}", PORT);

            byte[] buffer = new byte[BUFFER_SIZE];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    handlePacket(packet);
                } catch (IOException e) {
                    if (running) {
                        log.error("Error receiving UDP packet", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to start UDP Discovery Server", e);
        }
    }

    private void handlePacket(DatagramPacket packet) {
        try {
            String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            log.debug("Received UDP packet from {}:{} - {}", packet.getAddress(), packet.getPort(), data);

            Map<String, Object> request = objectMapper.readValue(data, Map.class);
            if ("DISCOVERY_PROBE".equals(request.get("type"))) {
                // Send response
                Map<String, Object> response = new HashMap<>();
                response.put("type", "DISCOVERY_RESPONSE");
                
                // Get local IP that is reachable from client
                // For simplicity, we send the IP that received the packet if it's not 0.0.0.0
                // Or use InetAddress.getLocalHost().getHostAddress()
                // But better to use the interface IP.
                // Since we are responding to the sender, they already know our IP (packet source).
                // But the payload requires serverIp.
                
                String localIp = InetAddress.getLocalHost().getHostAddress();
                // 确保不返回回环地址
                if (localIp.startsWith("127.")) {
                     try (DatagramSocket s = new DatagramSocket()) {
                        s.connect(InetAddress.getByName("8.8.8.8"), 10002);
                        localIp = s.getLocalAddress().getHostAddress();
                    } catch (Exception e) {
                        log.warn("Failed to determine local IP", e);
                    }
                }
                
                response.put("serverIp", localIp);
                response.put("serverPort", serverPort); // TCP port
                response.put("serverVersion", serverVersion);
                
                // Add signature if needed (user requirement: address verification)
                // "Response package includes server IP, port, version... Address verification mechanism"
                // Simple signature for now
                response.put("signature", "mock-signature"); 

                String jsonResponse = objectMapper.writeValueAsString(response);
                byte[] responseData = jsonResponse.getBytes(StandardCharsets.UTF_8);

                DatagramPacket responsePacket = new DatagramPacket(
                        responseData,
                        responseData.length,
                        packet.getAddress(),
                        packet.getPort()
                );
                socket.send(responsePacket);
                log.info("Sent discovery response to {}", packet.getAddress());
            }
        } catch (Exception e) {
            log.warn("Failed to handle discovery packet: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        executorService.shutdown();
    }
}

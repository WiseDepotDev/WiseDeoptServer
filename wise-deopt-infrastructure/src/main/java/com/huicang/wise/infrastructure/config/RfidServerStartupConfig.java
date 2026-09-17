package com.huicang.wise.infrastructure.config;

import com.huicang.wise.infrastructure.rfid.RfidTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * RFID TCP服务器启动配置
 * 在应用启动完成后自动启动RFID TCP服务器
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-18
 */
@Component
public class RfidServerStartupConfig {

    private static final Logger logger = LoggerFactory.getLogger(RfidServerStartupConfig.class);

    private final RfidTcpServer rfidTcpServer;

    public RfidServerStartupConfig(RfidTcpServer rfidTcpServer) {
        this.rfidTcpServer = rfidTcpServer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("应用启动完成，开始启动RFID TCP服务器...");
        
        Thread serverThread = new Thread(() -> {
            try {
                rfidTcpServer.start();
            } catch (Exception e) {
                logger.error("RFID TCP服务器启动失败", e);
            }
        });
        
        serverThread.setName("RFID-TCPServer");
        serverThread.setDaemon(true);
        serverThread.start();
        
        logger.info("RFID TCP服务器启动线程已创建");
    }
}

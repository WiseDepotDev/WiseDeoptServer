package com.huicang.wise.infrastructure.rfid;

import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RFID TCP服务器 监听RFID读写器的连接，接收RFID数据
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-18
 */
@Component
public class RfidTcpServer {

    private static final Logger logger = LoggerFactory.getLogger(RfidTcpServer.class);

    @Value("${rfid.server.port:6000}")
    private int serverPort;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final ConcurrentHashMap<String, RfidClientHandler> clientHandlers =
            new ConcurrentHashMap<>();
    private final DeviceRepository deviceRepository;
    private final RfidDataProcessor dataProcessor;

    public RfidTcpServer(DeviceRepository deviceRepository, RfidDataProcessor dataProcessor) {
        this.deviceRepository = deviceRepository;
        this.dataProcessor = dataProcessor;
    }

    public void start() {
        try {
            if (isPortInUse(serverPort)) {
                logger.warn("端口 {} 已被占用，尝试自动释放...", serverPort);

                int maxRetries = 3;
                boolean portReleased = false;

                for (int i = 0; i < maxRetries; i++) {
                    if (killProcessUsingPort(serverPort)) {
                        portReleased = true;
                        logger.info("端口 {} 已成功释放，等待端口完全释放...", serverPort);
                        break;
                    }

                    if (i < maxRetries - 1) {
                        logger.info("等待端口释放，重试 {}/{}", i + 1, maxRetries);
                        Thread.sleep(2000);
                    }
                }

                if (!portReleased) {
                    logger.error("经过 {} 次重试后仍无法释放端口 {}，RFID TCP服务器启动失败", maxRetries, serverPort);
                    logger.error("请手动检查并终止占用端口 {} 的进程", serverPort);
                    return;
                }

                logger.info("等待 {}ms 确保端口完全释放...", 2000);
                Thread.sleep(2000);
            }

            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(serverPort));
            executorService = Executors.newCachedThreadPool();

            logger.info("RFID TCP服务器启动成功，监听端口: {}", serverPort);

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientKey = getClientKey(clientSocket);

                    logger.info("RFID设备连接: {}", clientKey);

                    RfidClientHandler handler =
                            new RfidClientHandler(
                                    clientSocket, clientKey, dataProcessor, deviceRepository);
                    clientHandlers.put(clientKey, handler);
                    executorService.submit(handler);

                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        logger.error("接受客户端连接失败", e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("RFID TCP服务器启动失败", e);
        } catch (InterruptedException e) {
            logger.error("线程中断", e);
            Thread.currentThread().interrupt();
        }
    }

    private boolean isPortInUse(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private boolean killProcessUsingPort(int port) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Process process;

            if (os.contains("win")) {
                process =
                        Runtime.getRuntime()
                                .exec(String.format("netstat -ano | findstr :%d", port));
            } else {
                process = Runtime.getRuntime().exec(String.format("lsof -i :%d", port));
            }

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                List<String> pids = new ArrayList<>();

                logger.debug("开始解析端口占用信息，端口: {}", port);

                while ((line = reader.readLine()) != null) {
                    logger.debug("netstat输出行: {}", line);

                    if (os.contains("win")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 5) {
                            String portPart = parts[1];
                            if (portPart.contains(String.valueOf(port))) {
                                String pid = parts[parts.length - 1];
                                if (!pid.isEmpty() && !pid.equals("0")) {
                                    pids.add(pid);
                                    logger.info("发现占用端口 {} 的进程 PID: {}", port, pid);
                                }
                            }
                        }
                    } else {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            String pid = parts[1];
                            if (!pid.isEmpty()) {
                                pids.add(pid);
                                logger.info("发现占用端口 {} 的进程 PID: {}", port, pid);
                            }
                        }
                    }
                }

                if (!pids.isEmpty()) {
                    logger.info("发现占用端口 {} 的进程: {}", port, pids);

                    for (String pid : pids) {
                        try {
                            Process killProcess;
                            if (os.contains("win")) {
                                killProcess =
                                        Runtime.getRuntime()
                                                .exec(String.format("taskkill /F /PID %s", pid));
                            } else {
                                killProcess =
                                        Runtime.getRuntime().exec(String.format("kill -9 %s", pid));
                            }

                            int exitCode = killProcess.waitFor();
                            logger.info("已停止进程 PID: {}, 退出码: {}", pid, exitCode);

                        } catch (Exception e) {
                            logger.error("停止进程 PID {} 失败", pid, e);
                        }
                    }

                    return true;
                } else {
                    logger.warn("未发现占用端口 {} 的进程", port);
                }
            }

            return false;
        } catch (Exception e) {
            logger.error("检查端口占用失败", e);
            return false;
        }
    }

    @PreDestroy
    public void stop() {
        logger.info("正在停止RFID TCP服务器...");

        for (RfidClientHandler handler : clientHandlers.values()) {
            handler.close();
        }
        clientHandlers.clear();

        if (executorService != null) {
            executorService.shutdown();
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("关闭服务器socket失败", e);
            }
        }

        logger.info("RFID TCP服务器已停止");
    }

    private String getClientKey(Socket socket) {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public void removeClient(String clientKey) {
        RfidClientHandler handler = clientHandlers.remove(clientKey);
        if (handler != null) {
            logger.info("RFID设备断开连接: {}", clientKey);
        }
    }
}

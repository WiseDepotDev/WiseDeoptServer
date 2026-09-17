package com.huicang.wise.infrastructure.push;

import com.huicang.wise.domain.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockPushService implements PushService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockPushService.class);

    @Override
    public void sendPushNotification(Message message, String deviceToken) {
        logger.info("发送推送通知 - 消息ID: {}, 标题: {}, 接收者: {}, 设备Token: {}", 
            message.getId(), message.getTitle(), message.getReceiverId(), deviceToken);
    }

    @Override
    public void sendPushNotificationToUser(Long userId, String title, String content) {
        logger.info("发送推送通知 - 用户ID: {}, 标题: {}, 内容: {}", userId, title, content);
    }

    @Override
    public void broadcastNotification(String title, String content) {
        logger.info("广播推送通知 - 标题: {}, 内容: {}", title, content);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
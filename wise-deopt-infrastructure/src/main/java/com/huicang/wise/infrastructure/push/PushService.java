package com.huicang.wise.infrastructure.push;

import com.huicang.wise.domain.message.Message;

public interface PushService {
    void sendPushNotification(Message message, String deviceToken);
    void sendPushNotificationToUser(Long userId, String title, String content);
    void broadcastNotification(String title, String content);
    boolean isAvailable();
}
package com.huicang.wise.application.message;

import com.huicang.wise.domain.message.Message;
import com.huicang.wise.domain.message.MessageType;
import com.huicang.wise.infrastructure.push.PushService;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageApplicationService {

    private final ConcurrentHashMap<String, Message> messageStore = new ConcurrentHashMap<>();

    @Autowired
    private PushService pushService;

    @Transactional
    public Message createMessage(MessageCreateRequest request) {
        String messageId = UUID.randomUUID().toString();
        Message message = new Message(
            messageId,
            request.getTitle(),
            request.getContent(),
            request.getType().name(),
            request.getReceiverId()
        );
        
        message.setRelatedEntityType(request.getRelatedEntityType());
        message.setRelatedEntityId(request.getRelatedEntityId());
        message.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        
        messageStore.put(messageId, message);
        
        return message;
    }

    @Cacheable(prefix = "message", key = "#messageId", timeout = 1800)
    public Message getMessageById(String messageId) {
        return messageStore.get(messageId);
    }

    public List<Message> queryMessages(MessageQueryRequest request) {
        List<Message> result = new ArrayList<>();
        
        for (Message message : messageStore.values()) {
            if (request.getReceiverId() != null && 
                !message.getReceiverId().equals(request.getReceiverId())) {
                continue;
            }
            
            if (request.getType() != null && 
                !message.getType().equals(request.getType().name())) {
                continue;
            }
            
            if (request.getIsRead() != null && 
                !message.getIsRead().equals(request.getIsRead())) {
                continue;
            }
            
            result.add(message);
        }
        
        int start = request.getPage() * request.getSize();
        int end = Math.min(start + request.getSize(), result.size());
        
        if (start >= result.size()) {
            return new ArrayList<>();
        }
        
        return result.subList(start, end);
    }

    public int getUnreadCount(Long receiverId) {
        int count = 0;
        for (Message message : messageStore.values()) {
            if (message.getReceiverId().equals(receiverId) && 
                !message.getIsRead()) {
                count++;
            }
        }
        return count;
    }

    @Transactional
    public Message markAsRead(String messageId) {
        Message message = messageStore.get(messageId);
        if (message != null && !message.getIsRead()) {
            message.setIsRead(true);
            message.setReadTime(LocalDateTime.now());
        }
        return message;
    }

    @Transactional
    public void markAllAsRead(Long receiverId) {
        for (Message message : messageStore.values()) {
            if (message.getReceiverId().equals(receiverId) && 
                !message.getIsRead()) {
                message.setIsRead(true);
                message.setReadTime(LocalDateTime.now());
            }
        }
    }

    @Transactional
    public void deleteMessage(String messageId) {
        messageStore.remove(messageId);
    }

    @Transactional
    public void deleteAllMessages(Long receiverId) {
        messageStore.entrySet().removeIf(entry -> 
            entry.getValue().getReceiverId().equals(receiverId)
        );
    }

    public void sendPushNotification(Message message) {
        if (pushService.isAvailable()) {
            pushService.sendPushNotificationToUser(
                message.getReceiverId(),
                message.getTitle(),
                message.getContent()
            );
        }
    }
}
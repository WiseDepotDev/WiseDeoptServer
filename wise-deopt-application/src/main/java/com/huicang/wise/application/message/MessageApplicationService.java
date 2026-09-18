package com.huicang.wise.application.message;

import com.huicang.wise.domain.message.Message;
import com.huicang.wise.infrastructure.push.PushService;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageApplicationService {

    private final ConcurrentHashMap<String, Message> messageStore = new ConcurrentHashMap<>();

    @Autowired private PushService pushService;

    @Transactional
    public MessageDTO createMessage(MessageCreateRequest request) {
        String messageId = UUID.randomUUID().toString();
        Message message =
                new Message(
                        messageId,
                        request.getTitle(),
                        request.getContent(),
                        request.getType().name(),
                        request.getReceiverId());

        message.setRelatedEntityType(request.getRelatedEntityType());
        message.setRelatedEntityId(request.getRelatedEntityId());
        message.setPriority(request.getPriority() != null ? request.getPriority() : 0);

        messageStore.put(messageId, message);

        return toDto(message);
    }

    @Cacheable(prefix = "message", key = "#messageId", timeout = 1800)
    public MessageDTO getMessageById(String messageId) {
        return toDto(messageStore.get(messageId));
    }

    public List<MessageDTO> queryMessages(MessageQueryRequest request) {
        List<Message> matched = new ArrayList<>();

        for (Message message : messageStore.values()) {
            if (request.getReceiverId() != null
                    && !message.getReceiverId().equals(request.getReceiverId())) {
                continue;
            }

            if (request.getType() != null && !message.getType().equals(request.getType().name())) {
                continue;
            }

            if (request.getIsRead() != null && !message.getIsRead().equals(request.getIsRead())) {
                continue;
            }

            matched.add(message);
        }

        int start = request.getPage() * request.getSize();
        int end = Math.min(start + request.getSize(), matched.size());

        if (start >= matched.size()) {
            return new ArrayList<>();
        }

        return matched.subList(start, end).stream()
                .map(MessageApplicationService::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public int getUnreadCount(Long receiverId) {
        int count = 0;
        for (Message message : messageStore.values()) {
            if (message.getReceiverId().equals(receiverId) && !message.getIsRead()) {
                count++;
            }
        }
        return count;
    }

    @Transactional
    public MessageDTO markAsRead(String messageId) {
        Message message = messageStore.get(messageId);
        if (message != null && !message.getIsRead()) {
            message.setIsRead(true);
            message.setReadTime(LocalDateTime.now());
        }
        return toDto(message);
    }

    @Transactional
    public void markAllAsRead(Long receiverId) {
        for (Message message : messageStore.values()) {
            if (message.getReceiverId().equals(receiverId) && !message.getIsRead()) {
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
        messageStore
                .entrySet()
                .removeIf(entry -> entry.getValue().getReceiverId().equals(receiverId));
    }

    public void sendPushNotification(MessageDTO message) {
        if (pushService.isAvailable()) {
            pushService.sendPushNotificationToUser(
                    message.getReceiverId(), message.getTitle(), message.getContent());
        }
    }

    /**
     * 领域实体转 DTO（避免实体出现在接口签名，STD-NAME-02）。
     *
     * @param message 消息实体，可为 null
     * @return 消息 DTO；入参为 null 时返回 null
     */
    private static MessageDTO toDto(Message message) {
        if (message == null) {
            return null;
        }
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setTitle(message.getTitle());
        dto.setContent(message.getContent());
        dto.setType(message.getType());
        dto.setReceiverId(message.getReceiverId());
        dto.setReceiverName(message.getReceiverName());
        dto.setRelatedEntityType(message.getRelatedEntityType());
        dto.setRelatedEntityId(message.getRelatedEntityId());
        dto.setIsRead(message.getIsRead());
        dto.setReadTime(message.getReadTime());
        dto.setCreateTime(message.getCreateTime());
        dto.setStatus(message.getStatus());
        dto.setPriority(message.getPriority());
        return dto;
    }
}

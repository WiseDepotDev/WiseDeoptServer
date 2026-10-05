package com.huicang.wise.application.message;

import com.huicang.wise.domain.message.Message;
import com.huicang.wise.infrastructure.persistence.repository.message.MessageRepository;
import com.huicang.wise.infrastructure.push.PushService;
import com.huicang.wise.infrastructure.redis.RedisKeys;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内消息（8 类落点都经这里入库，然后由桥推给壳弹系统通知）。
 *
 * <p>## 2026-10-05：从"进程内 Map"改成数据库
 *
 * <p>改之前消息存在一个 `ConcurrentHashMap` 里：**服务端一重启消息全丢**， 多实例之间各看各的（同一用户在两个实例上看到的未读数不同）。消息是**业务事实** ——
 * 用户要能翻历史、要能标已读、桥的通知去重还要按 id 比较 —— 所以 owner 只能是数据库。 Redis 在这件事里只承担一件事：**未读数的短时缓存**（见 {@link
 * RedisKeys#MESSAGE_UNREAD}）。
 *
 * <p>## 缓存与失效是配对的
 *
 * <p>`getMessageById` 有 30 分钟缓存，所以**任何改动消息状态的写都必须让它失效**： 否则"标记已读"之后再去查，拿到的还是那份 `isRead=false` 的旧
 * DTO。 未读数同理 —— 它是全服务端最热的读（桥每 10s 轮询），但也是最容易被写坏的缓存。
 */
@Service
public class MessageApplicationService {

    private final MessageRepository messageRepository;

    @Autowired private PushService pushService;

    public MessageApplicationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 落库一条消息。
     *
     * <p>它同时失效该接收者的未读缓存：新增一条未读消息，"未读数"必然变了。 这里能精确到 `#request.receiverId`（参数里有接收者），所以不必整类清。
     */
    @CacheEvict(prefix = RedisKeys.MESSAGE_UNREAD, key = "#request.receiverId")
    @Transactional
    public MessageDTO createMessage(MessageCreateRequest request) {
        Message message =
                new Message(
                        UUID.randomUUID().toString(),
                        request.getTitle(),
                        request.getContent(),
                        request.getType().name(),
                        request.getReceiverId());

        message.setRelatedEntityType(request.getRelatedEntityType());
        message.setRelatedEntityId(request.getRelatedEntityId());
        message.setPriority(request.getPriority() != null ? request.getPriority() : 0);

        return toDto(messageRepository.save(message));
    }

    @Cacheable(
            prefix = RedisKeys.MESSAGE,
            key = "#messageId",
            timeout = 1800,
            unless = "#result == null")
    public MessageDTO getMessageById(String messageId) {
        return messageRepository
                .findById(messageId)
                .map(MessageApplicationService::toDto)
                .orElse(null);
    }

    /**
     * 按条件分页查询（新的在前）。
     *
     * <p>过滤与分页都下推到数据库：改之前是"遍历内存 Map 再手工切页"， 换成表之后那样写就会变成每次查询都把全部消息读进内存。
     */
    public List<MessageDTO> queryMessages(MessageQueryRequest request) {
        int page = request.getPage() == null ? 0 : Math.max(0, request.getPage());
        int size = request.getSize() == null || request.getSize() <= 0 ? 20 : request.getSize();

        List<Message> matched =
                messageRepository.findByConditions(
                        request.getReceiverId(),
                        request.getType() == null ? null : request.getType().name(),
                        request.getIsRead(),
                        PageRequest.of(page, size));

        return matched.stream().map(MessageApplicationService::toDto).collect(Collectors.toList());
    }

    /**
     * 未读数（桥每 10s 轮询一次）。
     *
     * <p>缓存 60 秒 + 所有写路径失效：这是"最热的读"与"最容易出错的缓存"之间的平衡点。 TTL 之所以还敢给 60 秒，是因为**正确性不依赖 TTL**，而是依赖写侧的
     * `@CacheEvict`。
     */
    @Cacheable(prefix = RedisKeys.MESSAGE_UNREAD, key = "#receiverId", timeout = 60)
    public int getUnreadCount(Long receiverId) {
        return (int) messageRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    /**
     * 标记单条已读。
     *
     * <p>失效用 `allEntries`：缓存键是"接收者"，而这个方法的参数只有 `messageId` —— 先查出接收者再按 key
     * 失效会多一次查询，而"标记已读"是低频操作、未读缓存又只是 60 秒的短缓存， 整类清（只清
     * `message:unread:*`）比多一次查询划算。注意这是**按前缀**清，不会碰到别的状态。
     */
    @CacheEvict(prefix = RedisKeys.MESSAGE_UNREAD, allEntries = true)
    @CacheEvict(prefix = RedisKeys.MESSAGE, key = "#messageId")
    @Transactional
    public MessageDTO markAsRead(String messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message != null && !Boolean.TRUE.equals(message.getIsRead())) {
            message.setIsRead(true);
            message.setReadTime(LocalDateTime.now());
            message = messageRepository.save(message);
        }
        return toDto(message);
    }

    /** 全部标记已读（一条 UPDATE 搞定，见仓库里的说明）。 */
    @CacheEvict(prefix = RedisKeys.MESSAGE_UNREAD, key = "#receiverId")
    @Transactional
    public void markAllAsRead(Long receiverId) {
        messageRepository.markAllAsRead(receiverId, LocalDateTime.now());
    }

    /** 删除单条（同样只有 messageId，所以未读缓存整类清）。 */
    @CacheEvict(prefix = RedisKeys.MESSAGE_UNREAD, allEntries = true)
    @CacheEvict(prefix = RedisKeys.MESSAGE, key = "#messageId")
    @Transactional
    public void deleteMessage(String messageId) {
        messageRepository.deleteById(messageId);
    }

    @CacheEvict(prefix = RedisKeys.MESSAGE_UNREAD, key = "#receiverId")
    @Transactional
    public void deleteAllMessages(Long receiverId) {
        messageRepository.deleteByReceiverId(receiverId);
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

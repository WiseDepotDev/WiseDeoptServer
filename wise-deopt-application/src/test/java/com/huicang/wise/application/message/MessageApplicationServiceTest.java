package com.huicang.wise.application.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.domain.message.Message;
import com.huicang.wise.domain.message.MessageType;
import com.huicang.wise.infrastructure.persistence.repository.message.MessageRepository;
import com.huicang.wise.infrastructure.push.PushService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link MessageApplicationService} 单元测试（P2-11；2026-10-05 改为数据库实现后同步重写）。
 *
 * <p>重点覆盖 P2-09 引入的「返回 DTO 而非领域实体」以及分页查询与已读标记的边界， 这些行为在重构中改动过，正是最需要回归保护的部分。
 *
 * <p>**为什么用"内存假仓库"而不是逐个 stub 方法**：这组用例要守的是**业务语义** （过滤、分页、已读、未读数），不是"有没有调用某个仓库方法"。用一个内存 Map 当成数据库，
 * 断言可以保持与改造前逐字相同 —— 这样"行为没变"这句话才有证据。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class MessageApplicationServiceTest {

    private MessageApplicationService service;

    private PushService pushService;

    private MessageRepository messageRepository;

    /** 假数据库：按插入顺序保存，`createTime` 由实体构造器写入。 */
    private final Map<String, Message> store = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();
        messageRepository = mock(MessageRepository.class);

        when(messageRepository.save(any(Message.class)))
                .thenAnswer(
                        invocation -> {
                            Message message = invocation.getArgument(0);
                            store.put(message.getId(), message);
                            return message;
                        });
        when(messageRepository.findById(anyString()))
                .thenAnswer(
                        invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
        when(messageRepository.countByReceiverIdAndIsReadFalse(anyLong()))
                .thenAnswer(
                        invocation -> {
                            Long receiverId = invocation.getArgument(0);
                            return store.values().stream()
                                    .filter(m -> receiverId.equals(m.getReceiverId()))
                                    .filter(m -> !Boolean.TRUE.equals(m.getIsRead()))
                                    .count();
                        });
        when(messageRepository.findByConditions(any(), any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            Long receiverId = invocation.getArgument(0);
                            String type = invocation.getArgument(1);
                            Boolean isRead = invocation.getArgument(2);
                            Pageable pageable = invocation.getArgument(3);
                            List<Message> matched =
                                    store.values().stream()
                                            .filter(
                                                    m ->
                                                            receiverId == null
                                                                    || receiverId.equals(
                                                                            m.getReceiverId()))
                                            .filter(m -> type == null || type.equals(m.getType()))
                                            .filter(
                                                    m ->
                                                            isRead == null
                                                                    || isRead.equals(m.getIsRead()))
                                            // 新消息在前（与仓库里的 ORDER BY create_time DESC 一致）
                                            .sorted(
                                                    (a, b) ->
                                                            b.getCreateTime()
                                                                    .compareTo(a.getCreateTime()))
                                            .collect(Collectors.toList());
                            int from = (int) pageable.getOffset();
                            if (from >= matched.size()) {
                                return new ArrayList<Message>();
                            }
                            int to = Math.min(from + pageable.getPageSize(), matched.size());
                            return new ArrayList<>(matched.subList(from, to));
                        });
        when(messageRepository.markAllAsRead(anyLong(), any(LocalDateTime.class)))
                .thenAnswer(
                        invocation -> {
                            Long receiverId = invocation.getArgument(0);
                            int changed = 0;
                            for (Message message : store.values()) {
                                if (receiverId.equals(message.getReceiverId())
                                        && !Boolean.TRUE.equals(message.getIsRead())) {
                                    message.setIsRead(true);
                                    message.setReadTime(invocation.getArgument(1));
                                    changed++;
                                }
                            }
                            return changed;
                        });
        when(messageRepository.deleteByReceiverId(anyLong()))
                .thenAnswer(
                        invocation -> {
                            Long receiverId = invocation.getArgument(0);
                            int before = store.size();
                            store.entrySet()
                                    .removeIf(
                                            entry ->
                                                    receiverId.equals(
                                                            entry.getValue().getReceiverId()));
                            return before - store.size();
                        });
        // deleteById 返回 void —— 只能 doAnswer，不能 when(...)
        doAnswer(
                        invocation -> {
                            store.remove(invocation.getArgument(0));
                            return null;
                        })
                .when(messageRepository)
                .deleteById(anyString());

        service = new MessageApplicationService(messageRepository);
        pushService = mock(PushService.class);
        ReflectionTestUtils.setField(service, "pushService", pushService);
    }

    /**
     * 构造一条创建请求。
     *
     * @param receiverId 接收人
     * @return 请求对象
     */
    private MessageCreateRequest newRequest(Long receiverId) {
        MessageCreateRequest request = new MessageCreateRequest();
        request.setTitle("标题");
        request.setContent("内容");
        request.setType(MessageType.ALERT);
        request.setReceiverId(receiverId);
        request.setPriority(1);
        return request;
    }

    /** 创建消息应返回 DTO（而非领域实体），字段逐项映射。 */
    @Test
    @DisplayName("shouldReturnDtoWhenCreateMessage")
    void shouldReturnDtoWhenCreateMessage() {
        MessageDTO dto = service.createMessage(newRequest(1L));

        assertNotNull(dto.getId(), "应生成消息ID");
        assertEquals("标题", dto.getTitle());
        assertEquals("内容", dto.getContent());
        assertEquals(MessageType.ALERT.name(), dto.getType());
        assertEquals(1L, dto.getReceiverId());
        assertEquals(1, dto.getPriority());
        assertFalse(dto.getIsRead() != null && dto.getIsRead(), "新建消息应为未读");
    }

    /** 按 ID 查询：命中返回 DTO，未命中返回 null。 */
    @Test
    @DisplayName("shouldReturnDtoOrNullWhenGetMessageById")
    void shouldReturnDtoOrNullWhenGetMessageById() {
        MessageDTO created = service.createMessage(newRequest(1L));

        MessageDTO found = service.getMessageById(created.getId());
        assertNotNull(found);
        assertEquals(created.getId(), found.getId());

        assertNull(service.getMessageById("not-exist"), "不存在的消息应返回 null");
    }

    /** 列表查询应返回 DTO 列表，并按接收人过滤。 */
    @Test
    @DisplayName("shouldReturnDtoListFilteredByReceiverWhenQueryMessages")
    void shouldReturnDtoListFilteredByReceiverWhenQueryMessages() {
        service.createMessage(newRequest(1L));
        service.createMessage(newRequest(1L));
        service.createMessage(newRequest(2L));

        MessageQueryRequest request = new MessageQueryRequest();
        request.setReceiverId(1L);
        request.setPage(0);
        request.setSize(10);

        List<MessageDTO> result = service.queryMessages(request);

        assertEquals(2, result.size(), "只应返回接收人 1 的消息");
        assertTrue(result.stream().allMatch(dto -> Long.valueOf(1L).equals(dto.getReceiverId())));
    }

    /** 分页参数应生效：起始页越界时返回空列表。 */
    @Test
    @DisplayName("shouldReturnEmptyWhenPageOutOfRange")
    void shouldReturnEmptyWhenPageOutOfRange() {
        service.createMessage(newRequest(1L));

        MessageQueryRequest request = new MessageQueryRequest();
        request.setReceiverId(1L);
        request.setPage(99);
        request.setSize(10);

        assertTrue(service.queryMessages(request).isEmpty(), "越界页应返回空列表");
    }

    /** 标记已读：返回 DTO 且状态变为已读并带时间。 */
    @Test
    @DisplayName("shouldMarkAsReadAndReturnDto")
    void shouldMarkAsReadAndReturnDto() {
        MessageDTO created = service.createMessage(newRequest(1L));

        MessageDTO read = service.markAsRead(created.getId());

        assertNotNull(read);
        assertTrue(Boolean.TRUE.equals(read.getIsRead()), "应标记为已读");
        assertNotNull(read.getReadTime(), "应记录已读时间");
    }

    /** 未读计数应按接收人统计。 */
    @Test
    @DisplayName("shouldCountUnreadByReceiver")
    void shouldCountUnreadByReceiver() {
        service.createMessage(newRequest(1L));
        service.createMessage(newRequest(1L));
        service.createMessage(newRequest(2L));

        assertEquals(2, service.getUnreadCount(1L));
        assertEquals(1, service.getUnreadCount(2L));
        assertEquals(0, service.getUnreadCount(99L));
    }

    /** 删除消息后应查不到。 */
    @Test
    @DisplayName("shouldRemoveMessageWhenDelete")
    void shouldRemoveMessageWhenDelete() {
        MessageDTO created = service.createMessage(newRequest(1L));

        service.deleteMessage(created.getId());

        assertNull(service.getMessageById(created.getId()));
    }

    /** 推送不可用时不得抛异常（旁路动作失败不应影响主流程）。 */
    @Test
    @DisplayName("shouldNotThrowWhenPushUnavailable")
    void shouldNotThrowWhenPushUnavailable() {
        MessageDTO created = service.createMessage(newRequest(1L));
        when(pushService.isAvailable()).thenReturn(false);

        service.sendPushNotification(created);
    }

    /** 全部标记已读：走一条 UPDATE，且未读数归零。 */
    @Test
    @DisplayName("shouldMarkAllAsReadForReceiver")
    void shouldMarkAllAsReadForReceiver() {
        service.createMessage(newRequest(1L));
        service.createMessage(newRequest(1L));
        service.createMessage(newRequest(2L));

        service.markAllAsRead(1L);

        assertEquals(0, service.getUnreadCount(1L), "接收人 1 应全部已读");
        assertEquals(1, service.getUnreadCount(2L), "接收人 2 不受影响");
        verify(messageRepository).markAllAsRead(eq(1L), any(LocalDateTime.class));
    }

    /** 删除某人的全部消息：只删他的。 */
    @Test
    @DisplayName("shouldDeleteAllMessagesOfOneReceiver")
    void shouldDeleteAllMessagesOfOneReceiver() {
        service.createMessage(newRequest(1L));
        service.createMessage(newRequest(1L));
        service.createMessage(newRequest(2L));

        service.deleteAllMessages(1L);

        assertEquals(0, service.getUnreadCount(1L));
        assertEquals(1, service.getUnreadCount(2L));
    }

    /** 越界页返回空（与改造前逐字一致的行为）。 */
    @Test
    @DisplayName("shouldReturnEmptyWhenPageBeyondRange")
    void shouldReturnEmptyWhenPageBeyondRange() {
        service.createMessage(newRequest(1L));

        MessageQueryRequest request = new MessageQueryRequest();
        request.setReceiverId(1L);
        request.setPage(99);
        request.setSize(10);

        assertTrue(service.queryMessages(request).isEmpty());
    }
}

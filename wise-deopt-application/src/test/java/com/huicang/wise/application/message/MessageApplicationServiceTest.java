package com.huicang.wise.application.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.huicang.wise.domain.message.MessageType;
import com.huicang.wise.infrastructure.push.PushService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link MessageApplicationService} 单元测试（P2-11）。
 *
 * <p>重点覆盖 P2-09 引入的「返回 DTO 而非领域实体」以及分页查询与已读标记的边界， 这些行为在重构中改动过，正是最需要回归保护的部分。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class MessageApplicationServiceTest {

    private MessageApplicationService service;

    private PushService pushService;

    @BeforeEach
    void setUp() {
        service = new MessageApplicationService();
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
}

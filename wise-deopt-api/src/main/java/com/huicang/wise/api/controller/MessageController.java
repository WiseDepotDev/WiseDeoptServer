package com.huicang.wise.api.controller;

import com.huicang.wise.application.message.MessageApplicationService;
import com.huicang.wise.application.message.MessageCreateRequest;
import com.huicang.wise.application.message.MessageDTO;
import com.huicang.wise.application.message.MessageQueryRequest;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.api.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired private MessageApplicationService messageApplicationService;

    @PostMapping
    public ApiResponse<MessageDTO> createMessage(@RequestBody MessageCreateRequest request) {
        MessageDTO message = messageApplicationService.createMessage(request);
        return ApiResponse.success(message);
    }

    @GetMapping("/{messageId}")
    public ApiResponse<MessageDTO> getMessage(@PathVariable String messageId) {
        MessageDTO message = messageApplicationService.getMessageById(messageId);
        if (message == null) {
            return ApiResponse.failure(ErrorCode.NOT_FOUND, "消息不存在");
        }
        return ApiResponse.success(message);
    }

    @GetMapping
    public ApiResponse<List<MessageDTO>> queryMessages(MessageQueryRequest request) {
        List<MessageDTO> messages = messageApplicationService.queryMessages(request);
        return ApiResponse.success(messages);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount(@RequestParam Long receiverId) {
        int count = messageApplicationService.getUnreadCount(receiverId);
        return ApiResponse.success(count);
    }

    @PutMapping("/{messageId}/read")
    public ApiResponse<MessageDTO> markAsRead(@PathVariable String messageId) {
        MessageDTO message = messageApplicationService.markAsRead(messageId);
        if (message == null) {
            return ApiResponse.failure(ErrorCode.NOT_FOUND, "消息不存在");
        }
        return ApiResponse.success(message);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@RequestParam Long receiverId) {
        messageApplicationService.markAllAsRead(receiverId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> deleteMessage(@PathVariable String messageId) {
        messageApplicationService.deleteMessage(messageId);
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAllMessages(@RequestParam Long receiverId) {
        messageApplicationService.deleteAllMessages(receiverId);
        return ApiResponse.success(null);
    }
}

package com.huicang.wise.api.controller;

import com.huicang.wise.application.message.MessageApplicationService;
import com.huicang.wise.application.message.MessageCreateRequest;
import com.huicang.wise.application.message.MessageQueryRequest;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.domain.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageApplicationService messageApplicationService;

    @PostMapping
    public ApiResponse<Message> createMessage(@RequestBody MessageCreateRequest request) {
        Message message = messageApplicationService.createMessage(request);
        return ApiResponse.success(message);
    }

    @GetMapping("/{messageId}")
    public ApiResponse<Message> getMessage(@PathVariable String messageId) {
        Message message = messageApplicationService.getMessageById(messageId);
        if (message == null) {
            return ApiResponse.failure(ErrorCode.NOT_FOUND, "消息不存在");
        }
        return ApiResponse.success(message);
    }

    @GetMapping
    public ApiResponse<List<Message>> queryMessages(MessageQueryRequest request) {
        List<Message> messages = messageApplicationService.queryMessages(request);
        return ApiResponse.success(messages);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount(@RequestParam Long receiverId) {
        int count = messageApplicationService.getUnreadCount(receiverId);
        return ApiResponse.success(count);
    }

    @PutMapping("/{messageId}/read")
    public ApiResponse<Message> markAsRead(@PathVariable String messageId) {
        Message message = messageApplicationService.markAsRead(messageId);
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
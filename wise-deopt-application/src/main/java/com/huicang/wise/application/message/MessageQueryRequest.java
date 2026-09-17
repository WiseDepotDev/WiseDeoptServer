package com.huicang.wise.application.message;

import com.huicang.wise.domain.message.MessageType;

public class MessageQueryRequest {
    private Long receiverId;
    private MessageType type;
    private Boolean isRead;
    private Integer page;
    private Integer size;

    public MessageQueryRequest() {
        this.page = 0;
        this.size = 20;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
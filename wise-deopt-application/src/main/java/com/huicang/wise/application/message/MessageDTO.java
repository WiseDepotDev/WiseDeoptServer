package com.huicang.wise.application.message;

import java.time.LocalDateTime;

/**
 * 消息出参对象（对外传输，避免领域实体出现在接口签名，STD-NAME-02）。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
public class MessageDTO {

    /** 消息ID */
    private String id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 消息类型 */
    private String type;

    /** 接收人ID */
    private Long receiverId;

    /** 接收人名称 */
    private String receiverName;

    /** 关联实体类型 */
    private String relatedEntityType;

    /** 关联实体ID */
    private String relatedEntityId;

    /** 是否已读 */
    private Boolean isRead;

    /** 已读时间 */
    private LocalDateTime readTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 消息状态 */
    private String status;

    /** 优先级 */
    private Integer priority;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(String relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}

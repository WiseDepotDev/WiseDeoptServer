package com.huicang.wise.domain.sync;

import java.time.LocalDateTime;

public class SyncOperation {
    private String id;
    private String entityType;
    private String entityId;
    private String operationType;
    private String operationData;
    private LocalDateTime operationTime;
    private String deviceId;
    private String status;
    private LocalDateTime syncTime;
    private String errorMessage;
    private Integer version;

    public SyncOperation() {
    }

    public SyncOperation(String id, String entityType, String entityId, String operationType, 
                         String operationData, LocalDateTime operationTime, String deviceId) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.operationType = operationType;
        this.operationData = operationData;
        this.operationTime = operationTime;
        this.deviceId = deviceId;
        this.status = "PENDING";
        this.version = 1;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationData() {
        return operationData;
    }

    public void setOperationData(String operationData) {
        this.operationData = operationData;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(LocalDateTime syncTime) {
        this.syncTime = syncTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
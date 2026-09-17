package com.huicang.wise.application.sync;

import com.huicang.wise.domain.sync.SyncOperation;
import java.time.LocalDateTime;
import java.util.List;

public class SyncResponse {
    private boolean success;
    private String message;
    private LocalDateTime syncTime;
    private List<SyncOperation> serverOperations;
    private List<SyncOperation> failedOperations;
    private List<String> conflicts;

    public SyncResponse() {
    }

    public SyncResponse(boolean success, String message, LocalDateTime syncTime) {
        this.success = success;
        this.message = message;
        this.syncTime = syncTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(LocalDateTime syncTime) {
        this.syncTime = syncTime;
    }

    public List<SyncOperation> getServerOperations() {
        return serverOperations;
    }

    public void setServerOperations(List<SyncOperation> serverOperations) {
        this.serverOperations = serverOperations;
    }

    public List<SyncOperation> getFailedOperations() {
        return failedOperations;
    }

    public void setFailedOperations(List<SyncOperation> failedOperations) {
        this.failedOperations = failedOperations;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts;
    }
}
package com.huicang.wise.application.sync;

import com.huicang.wise.domain.sync.SyncOperation;
import java.time.LocalDateTime;
import java.util.List;

public class SyncRequest {
    private String deviceId;
    private LocalDateTime lastSyncTime;
    private List<SyncOperation> operations;

    public SyncRequest() {}

    public SyncRequest(
            String deviceId, LocalDateTime lastSyncTime, List<SyncOperation> operations) {
        this.deviceId = deviceId;
        this.lastSyncTime = lastSyncTime;
        this.operations = operations;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public List<SyncOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<SyncOperation> operations) {
        this.operations = operations;
    }
}

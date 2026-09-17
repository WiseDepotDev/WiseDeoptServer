package com.huicang.wise.application.sync;

import com.huicang.wise.domain.sync.ConflictResolutionStrategy;
import com.huicang.wise.domain.sync.SyncOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SyncApplicationService {

    @Transactional
    public SyncResponse syncData(SyncRequest request) {
        SyncResponse response = new SyncResponse();
        LocalDateTime syncTime = LocalDateTime.now();
        List<SyncOperation> failedOperations = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        try {
            for (SyncOperation operation : request.getOperations()) {
                try {
                    processOperation(operation, request.getDeviceId());
                } catch (Exception e) {
                    operation.setStatus("FAILED");
                    operation.setErrorMessage(e.getMessage());
                    failedOperations.add(operation);
                }
            }

            List<SyncOperation> serverOperations = getServerOperations(request.getLastSyncTime());

            response.setSuccess(true);
            response.setMessage("同步成功");
            response.setSyncTime(syncTime);
            response.setServerOperations(serverOperations);
            response.setFailedOperations(failedOperations);
            response.setConflicts(conflicts);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("同步失败: " + e.getMessage());
        }

        return response;
    }

    private void processOperation(SyncOperation operation, String deviceId) {
        switch (operation.getOperationType()) {
            case "CREATE":
                handleCreateOperation(operation);
                break;
            case "UPDATE":
                handleUpdateOperation(operation, ConflictResolutionStrategy.LAST_WRITE_WINS);
                break;
            case "DELETE":
                handleDeleteOperation(operation);
                break;
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + operation.getOperationType());
        }

        operation.setStatus("COMPLETED");
        operation.setSyncTime(LocalDateTime.now());
    }

    private void handleCreateOperation(SyncOperation operation) {
        String entityId = UUID.randomUUID().toString();
        operation.setEntityId(entityId);
    }

    private void handleUpdateOperation(SyncOperation operation, ConflictResolutionStrategy strategy) {
        switch (strategy) {
            case SERVER_WINS:
                break;
            case CLIENT_WINS:
                break;
            case LAST_WRITE_WINS:
                break;
            case MERGE:
                break;
            default:
                throw new IllegalArgumentException("不支持的冲突解决策略: " + strategy);
        }
    }

    private void handleDeleteOperation(SyncOperation operation) {
    }

    private List<SyncOperation> getServerOperations(LocalDateTime lastSyncTime) {
        return new ArrayList<>();
    }

    public SyncOperation resolveConflict(SyncOperation clientOperation, SyncOperation serverOperation, 
                                         ConflictResolutionStrategy strategy) {
        switch (strategy) {
            case SERVER_WINS:
                return serverOperation;
            case CLIENT_WINS:
                return clientOperation;
            case LAST_WRITE_WINS:
                if (clientOperation.getOperationTime().isAfter(serverOperation.getOperationTime())) {
                    return clientOperation;
                }
                return serverOperation;
            case MERGE:
                return mergeOperations(clientOperation, serverOperation);
            default:
                throw new IllegalArgumentException("不支持的冲突解决策略: " + strategy);
        }
    }

    private SyncOperation mergeOperations(SyncOperation clientOperation, SyncOperation serverOperation) {
        SyncOperation merged = new SyncOperation();
        merged.setId(serverOperation.getId());
        merged.setEntityType(serverOperation.getEntityType());
        merged.setEntityId(serverOperation.getEntityId());
        merged.setOperationType("UPDATE");
        merged.setOperationData(mergeData(clientOperation.getOperationData(), serverOperation.getOperationData()));
        merged.setOperationTime(LocalDateTime.now());
        merged.setDeviceId(serverOperation.getDeviceId());
        merged.setStatus("COMPLETED");
        merged.setVersion(Math.max(clientOperation.getVersion(), serverOperation.getVersion()) + 1);
        return merged;
    }

    private String mergeData(String clientData, String serverData) {
        return clientData;
    }
}
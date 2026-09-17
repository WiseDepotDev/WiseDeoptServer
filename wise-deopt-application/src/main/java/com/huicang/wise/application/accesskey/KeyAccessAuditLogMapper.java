package com.huicang.wise.application.accesskey;

import com.huicang.wise.domain.auth.KeyAccessAuditLog;
import org.springframework.stereotype.Component;

@Component
public class KeyAccessAuditLogMapper {

    public AccessKeyAuditLogDTO toDTO(KeyAccessAuditLog entity) {
        if (entity == null) {
            return null;
        }

        AccessKeyAuditLogDTO dto = new AccessKeyAuditLogDTO();
        dto.setLogId(entity.getLogId());
        dto.setUserId(entity.getUserId());
        dto.setAccessKey(entity.getAccessKey());
        dto.setRequestUri(entity.getRequestUri());
        dto.setMethod(entity.getMethod());
        dto.setIpAddress(entity.getIpAddress());
        dto.setStatusCode(entity.getStatusCode());
        dto.setResultMessage(entity.getResultMessage());
        dto.setDurationMs(entity.getDurationMs());
        dto.setRequestTime(entity.getRequestTime());

        return dto;
    }

    public KeyAccessAuditLog toEntity(AccessKeyAuditLogDTO dto) {
        if (dto == null) {
            return null;
        }

        KeyAccessAuditLog entity = new KeyAccessAuditLog();
        entity.setLogId(dto.getLogId());
        entity.setUserId(dto.getUserId());
        entity.setAccessKey(dto.getAccessKey());
        entity.setRequestUri(dto.getRequestUri());
        entity.setMethod(dto.getMethod());
        entity.setIpAddress(dto.getIpAddress());
        entity.setStatusCode(dto.getStatusCode());
        entity.setResultMessage(dto.getResultMessage());
        entity.setDurationMs(dto.getDurationMs());
        entity.setRequestTime(dto.getRequestTime());

        return entity;
    }
}

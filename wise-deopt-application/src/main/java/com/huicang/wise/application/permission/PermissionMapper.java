package com.huicang.wise.application.permission;

import com.huicang.wise.domain.auth.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionDTO toDTO(Permission entity) {
        if (entity == null) {
            return null;
        }

        PermissionDTO dto = new PermissionDTO();
        dto.setPermissionId(entity.getPermissionId());
        dto.setPermissionName(entity.getName());
        dto.setPermissionCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreateTime());
        dto.setUpdatedAt(entity.getUpdateTime());

        return dto;
    }

    public Permission toEntity(PermissionDTO dto) {
        if (dto == null) {
            return null;
        }

        Permission entity = new Permission();
        entity.setPermissionId(dto.getPermissionId());
        entity.setName(dto.getPermissionName());
        entity.setCode(dto.getPermissionCode());
        entity.setDescription(dto.getDescription());
        entity.setCreateTime(dto.getCreatedAt());
        entity.setUpdateTime(dto.getUpdatedAt());

        return entity;
    }
}

package com.huicang.wise.application.accesskey;

import com.huicang.wise.domain.user.UserAccessKey;
import org.springframework.stereotype.Component;

@Component
public class AccessKeyMapper {

    public AccessKeyDTO toDTO(UserAccessKey entity) {
        if (entity == null) {
            return null;
        }

        AccessKeyDTO dto = new AccessKeyDTO();
        dto.setKeyId(entity.getKeyId());
        dto.setUserId(entity.getUserId());
        dto.setAccessKey(entity.getAccessKey());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setCreateBy(entity.getCreateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setUpdateTime(entity.getUpdateTime());

        return dto;
    }

    public UserAccessKey toEntity(AccessKeyDTO dto) {
        if (dto == null) {
            return null;
        }

        UserAccessKey entity = new UserAccessKey();
        entity.setKeyId(dto.getKeyId());
        entity.setUserId(dto.getUserId());
        entity.setAccessKey(dto.getAccessKey());
        entity.setStatus(dto.getStatus());
        entity.setDescription(dto.getDescription());
        entity.setCreateBy(dto.getCreateBy());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateBy(dto.getUpdateBy());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }
}

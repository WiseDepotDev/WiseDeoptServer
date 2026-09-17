package com.huicang.wise.application.role;

import com.huicang.wise.domain.auth.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleDTO toDTO(Role entity) {
        if (entity == null) {
            return null;
        }

        RoleDTO dto = new RoleDTO();
        dto.setRoleId(entity.getRoleId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCreateTime(entity.getCreateTime());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setRoleCode(mapRoleNameToCode(entity.getName()));

        return dto;
    }

    private String mapRoleNameToCode(String roleName) {
        if (roleName == null) {
            return "USER";
        }
        switch (roleName) {
            case "超级管理员":
            case "管理员":
                return "ADMIN";
            case "操作员":
            case "访客":
            default:
                return "USER";
        }
    }

    public Role toEntity(RoleDTO dto) {
        if (dto == null) {
            return null;
        }

        Role entity = new Role();
        entity.setRoleId(dto.getRoleId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCreateTime(dto.getCreateTime());
        entity.setCreateBy(dto.getCreateBy());
        entity.setUpdateTime(dto.getUpdateTime());
        entity.setUpdateBy(dto.getUpdateBy());

        return entity;
    }
}

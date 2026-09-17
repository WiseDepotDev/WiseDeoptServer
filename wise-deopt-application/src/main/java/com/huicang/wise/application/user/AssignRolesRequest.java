package com.huicang.wise.application.user;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AssignRolesRequest {
    @NotNull(message = "角色ID列表不能为空")
    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}

package com.huicang.wise.application.role;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AssignPermissionsRequest {
    @NotNull(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
    private Long createBy;

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }
}

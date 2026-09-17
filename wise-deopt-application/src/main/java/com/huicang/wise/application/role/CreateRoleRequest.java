package com.huicang.wise.application.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateRoleRequest {
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称长度不能超过32个字符")
    private String name;
    @Size(max = 255, message = "角色描述长度不能超过255个字符")
    private String description;
    private Long createBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }
}

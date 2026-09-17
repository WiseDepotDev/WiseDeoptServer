package com.huicang.wise.application.accesskey;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateAccessKeyRequest {
    @Size(max = 128, message = "用途说明长度不能超过128个字符")
    private String description;
    @NotNull(message = "状态不能为空")
    private Short status;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }
}

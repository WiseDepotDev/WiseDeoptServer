package com.huicang.wise.application.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类功能描述：修改密码请求
 *
 * @author xingchentye
 * @date 2026-01-29
 */
@Data
@Schema(description = "修改密码请求")
public class UserPasswordChangeRequest {

    @Schema(description = "旧密码", required = true)
    private String oldPassword;

    @Schema(description = "新密码", required = true)
    private String newPassword;
}


package com.huicang.wise.application.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 类功能描述：修改密码请求
 *
 * @author xingchentye
 * @date 2026-01-29
 */
@Data
@ApiModel(description = "修改密码请求")
public class UserPasswordChangeRequest {

    @ApiModelProperty(value = "旧密码", required = true)
    private String oldPassword;

    @ApiModelProperty(value = "新密码", required = true)
    private String newPassword;
}

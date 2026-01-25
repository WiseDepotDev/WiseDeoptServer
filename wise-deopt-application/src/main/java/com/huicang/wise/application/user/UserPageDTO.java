package com.huicang.wise.application.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

/**
 * 类功能描述：用户分页DTO
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@ApiModel(description = "用户分页DTO")
public class UserPageDTO {

    @ApiModelProperty(value = "总记录数")
    private Long total;

    @ApiModelProperty(value = "用户列表")
    private List<UserDTO> items;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<UserDTO> getItems() {
        return items;
    }

    public void setItems(List<UserDTO> items) {
        this.items = items;
    }
}

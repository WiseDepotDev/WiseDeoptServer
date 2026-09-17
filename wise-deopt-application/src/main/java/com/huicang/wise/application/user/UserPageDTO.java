package com.huicang.wise.application.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 类功能描述：用户分页DTO
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Schema(description = "用户分页DTO")
public class UserPageDTO {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "用户列表")
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


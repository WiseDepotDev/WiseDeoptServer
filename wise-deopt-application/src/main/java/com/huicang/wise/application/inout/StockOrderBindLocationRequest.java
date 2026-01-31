package com.huicang.wise.application.inout;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 类功能描述：入库单绑定货位请求
 *
 * @author xingchentye
 * @version 1.0
 */
@Data
@ApiModel(description = "入库单绑定货位请求")
public class StockOrderBindLocationRequest {

    @ApiModelProperty(value = "绑定明细列表", required = true)
    private List<BindItem> items;

    @Data
    @ApiModel(description = "绑定货位明细")
    public static class BindItem {
        @ApiModelProperty(value = "产品ID", required = true)
        private Long productId;

        @ApiModelProperty(value = "货位编码", required = true)
        private String locationCode;
    }
}

package com.huicang.wise.application.inout;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 类功能描述：入库单绑定货位请求
 *
 * @author xingchentye
 * @version 1.0
 */
@Data
@Schema(description = "入库单绑定货位请求")
public class StockOrderBindLocationRequest {

    @Schema(description = "绑定明细列表", required = true)
    private List<BindItem> items;

    @Data
    @Schema(description = "绑定货位明细")
    public static class BindItem {
        @Schema(description = "产品ID", required = true)
        private Long productId;

        @Schema(description = "货位编码", required = true)
        private String locationCode;
    }
}


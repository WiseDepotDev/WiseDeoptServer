package com.huicang.wise.application.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

/**
 * 批量绑定标签请求
 */
@ApiModel("批量绑定标签请求")
public class ProductTagBatchBindRequest {

    @ApiModelProperty("目标产品ID（若设置，则覆盖列表中的产品ID）")
    private Long productId;

    @ApiModelProperty("待绑定标签列表")
    private List<TagBindInfo> tags;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public List<TagBindInfo> getTags() {
        return tags;
    }

    public void setTags(List<TagBindInfo> tags) {
        this.tags = tags;
    }

    @ApiModel("标签绑定信息")
    public static class TagBindInfo {
        @ApiModelProperty("条码")
        private String barcode;
        @ApiModelProperty("NFC UID")
        private String nfcUid;
        @ApiModelProperty("RFID")
        private String rfid;

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public String getNfcUid() {
            return nfcUid;
        }

        public void setNfcUid(String nfcUid) {
            this.nfcUid = nfcUid;
        }

        public String getRfid() {
            return rfid;
        }

        public void setRfid(String rfid) {
            this.rfid = rfid;
        }
    }
}

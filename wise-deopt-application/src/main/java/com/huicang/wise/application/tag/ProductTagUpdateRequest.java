package com.huicang.wise.application.tag;

/**
 * 类功能描述：产品标签更新请求
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-03-03
 */
public class ProductTagUpdateRequest {

    private Long productId;

    private Short status;

    private String barcode;

    private String nfcUid;

    private String rfid;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

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
package com.huicang.wise.application.tag;

/**
 * 类功能描述：产品标签创建请求
 *
 * @author xingchentye
 * @date 2026-02-27
 */
public class ProductTagCreateRequest {

    private Long productId;

    private String barcode;

    private String nfcUid;

    private String rfid;

    private Short status;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }
}
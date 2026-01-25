package com.huicang.wise.application.auth;

/**
 * 类功能描述：NFC登录请求参数
 *
 * @author xingchentye
 * @date 2026-01-22
 */
public class UserNfcLoginDTO {

    /**
     * 方法功能描述：NFC ID
     */
    private String nfcId;

    public String getNfcId() {
        return nfcId;
    }

    public void setNfcId(String nfcId) {
        this.nfcId = nfcId;
    }
}

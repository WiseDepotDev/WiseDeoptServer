package com.huicang.wise.application.auth;

/**
 * 类功能描述：NFC+PIN登录请求参数
 *
 * @author xingchentye
 * @date 2026-01-22
 */
public class NfcPinDTO {

    /**
     * 方法功能描述：NFC ID
     */
    private String nfcId;

    /**
     * 方法功能描述：PIN码
     */
    private String pin;

    public String getNfcId() {
        return nfcId;
    }

    public void setNfcId(String nfcId) {
        this.nfcId = nfcId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}

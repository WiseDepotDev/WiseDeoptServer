package com.huicang.wise.application.alert;

/**
 * 类功能描述：RFID-视频一致性告警请求
 *
 * @author WiseDepot
 * @version 0.1.17
 * @since 2026-02-27
 */
public class RfidVideoConsistencyRequest {

    private Integer rfidCount;
    private Integer videoCount;
    private String location;

    public Integer getRfidCount() {
        return rfidCount;
    }

    public void setRfidCount(Integer rfidCount) {
        this.rfidCount = rfidCount;
    }

    public Integer getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(Integer videoCount) {
        this.videoCount = videoCount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}

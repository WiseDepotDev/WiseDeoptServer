package com.huicang.wise.domain.inspection;

import java.io.Serializable;
import java.util.Map;

/**
 * 巡检进度更新事件
 */
public class InspectionProgressEvent implements Serializable {
    private Long taskId;
    private Integer progress;
    private String status;
    private Integer totalScanned;
    private Integer totalExpected;

    public InspectionProgressEvent() {}

    public InspectionProgressEvent(Long taskId, Integer progress, String status, Integer totalScanned, Integer totalExpected) {
        this.taskId = taskId;
        this.progress = progress;
        this.status = status;
        this.totalScanned = totalScanned;
        this.totalExpected = totalExpected;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalScanned() {
        return totalScanned;
    }

    public void setTotalScanned(Integer totalScanned) {
        this.totalScanned = totalScanned;
    }

    public Integer getTotalExpected() {
        return totalExpected;
    }

    public void setTotalExpected(Integer totalExpected) {
        this.totalExpected = totalExpected;
    }
}

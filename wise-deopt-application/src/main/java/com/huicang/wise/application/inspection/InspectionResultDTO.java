package com.huicang.wise.application.inspection;

import java.time.LocalDateTime;

public class InspectionResultDTO {
    private Long resultId;
    private Long taskId;
    private LocalDateTime compareTime;
    private Integer totalItems;
    private Integer normalItems;
    private Integer missingItems;
    private Integer extraItems;
    private LocalDateTime createTime;
    private Integer progress;
    private String status;
    private Integer totalScanned;
    private Integer totalExpected;

    public Long getResultId() { return resultId; }
    public void setResultId(Long resultId) { this.resultId = resultId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public LocalDateTime getCompareTime() { return compareTime; }
    public void setCompareTime(LocalDateTime compareTime) { this.compareTime = compareTime; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public Integer getNormalItems() { return normalItems; }
    public void setNormalItems(Integer normalItems) { this.normalItems = normalItems; }
    public Integer getMissingItems() { return missingItems; }
    public void setMissingItems(Integer missingItems) { this.missingItems = missingItems; }
    public Integer getExtraItems() { return extraItems; }
    public void setExtraItems(Integer extraItems) { this.extraItems = extraItems; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalScanned() { return totalScanned; }
    public void setTotalScanned(Integer totalScanned) { this.totalScanned = totalScanned; }
    public Integer getTotalExpected() { return totalExpected; }
    public void setTotalExpected(Integer totalExpected) { this.totalExpected = totalExpected; }
}
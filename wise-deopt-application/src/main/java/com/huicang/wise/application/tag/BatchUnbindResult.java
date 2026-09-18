package com.huicang.wise.application.tag;

/**
 * 类功能描述：批量解绑结果
 *
 * @author xingchentye
 * @date 2026-02-27
 */
public class BatchUnbindResult {

    private Integer totalCount;

    private Integer successCount;

    private Integer failedCount;

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }
}

package com.huicang.wise.application.inventory;

/**
 * 类功能描述：复核差异请求
 *
 * @author xingchentye
 * @date 2026-01-25
 */
public class ReviewDifferenceRequest {

    /**
     * 处理动作：CONFIRM (确认差异/标为已处理), CORRECT (修正库存), IGNORE (忽略)
     */
    private String action;

    /**
     * 备注
     */
    private String remark;

    /**
     * 目标库位（用于移位操作）
     */
    private String targetLocation;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(String targetLocation) {
        this.targetLocation = targetLocation;
    }
}

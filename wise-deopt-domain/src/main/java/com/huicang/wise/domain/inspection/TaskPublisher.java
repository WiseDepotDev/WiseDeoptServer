package com.huicang.wise.domain.inspection;

/**
 * 任务发布接口
 */
public interface TaskPublisher {
    /**
     * 发布巡检任务到设备
     *
     * @param deviceCode 设备编码
     * @param task       巡检任务消息
     */
    void publishTask(String deviceCode, TaskMessage task);
}

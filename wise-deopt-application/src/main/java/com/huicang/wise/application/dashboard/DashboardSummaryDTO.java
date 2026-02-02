package com.huicang.wise.application.dashboard;

import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.task.TaskDTO;
import lombok.Data;

import java.util.List;

/**
 * 类功能描述：首页看板摘要DTO
 *
 * @author xingchentye
 * @since 2026-02-02
 */
@Data
public class DashboardSummaryDTO {

    /**
     * 库存总量
     */
    private Long inventoryTotal;

    /**
     * 今日报警数
     */
    private Long todayAlertCount;

    /**
     * 巡检进度/状态
     */
    private Integer inspectionProgress;

    /**
     * 设备在线数
     */
    private Long deviceOnlineCount;

    /**
     * 未处理告警列表 (Top 3-5)
     */
    private List<AlertDTO> unprocessedAlerts;

    /**
     * 当前进行中的任务
     */
    private TaskDTO currentTask;
}

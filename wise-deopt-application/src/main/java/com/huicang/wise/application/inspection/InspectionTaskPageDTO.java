package com.huicang.wise.application.inspection;

import java.util.List;

/**
 * 类功能描述：巡检任务分页响应对象
 *
 * @author WiseDepot
 * @date 2026-03-17
 */
public class InspectionTaskPageDTO {

    /** 字段功能描述：总记录数 */
    private Long total;

    /** 字段功能描述：巡检任务列表 */
    private List<InspectionTaskDTO> rows;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<InspectionTaskDTO> getRows() {
        return rows;
    }

    public void setRows(List<InspectionTaskDTO> rows) {
        this.rows = rows;
    }
}

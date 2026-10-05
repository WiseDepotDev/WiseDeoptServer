package com.huicang.wise.application.alert;

/**
 * 更新告警状态的请求体。
 *
 * <p><b>这里刻意没有"操作人"字段</b>（2026-10-05）。
 *
 * <p>它原来有一个 `handlerId`，服务端把它直接写进处理记录（`alert_handle_log.handler_id`， 数据库上 `NOT
 * NULL`）。而客户端**没有**这个值可用（页面凭什么知道当前用户 id？）， 于是每次"处理完成 / 忽略"都在写库那一刻被 Bean Validation 拦下： `操作人id不能为空`
 * —— 用户看到的就是这句话，它既不像参数错误、也没告诉他该怎么办。
 *
 * <p>更要紧的是**审计正确性**：操作人由请求体提供，等于让调用方自己申报"这事是我干的"， 谁都能把处置记录记到别人头上。所以操作人改由服务端从**认证上下文**取 （见
 * `AlertController` 里的 `request.getAttribute("userId")`）。
 */
public class UpdateAlertStatusRequest {

    private Integer status;

    private String remark;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

package com.huicang.wise.application.common;

import lombok.Data;

@Data
public class DeleteWithVerifyRequest {
    private Long id;

    /** 人机验证票据（一次性，由桥按用途注入）。缺失即失败。 */
    private String humanToken;

    public String getHumanToken() {
        return humanToken;
    }

    public void setHumanToken(String humanToken) {
        this.humanToken = humanToken;
    }
}

package com.huicang.wise.application.human;

/**
 * 人机验证通过后下发的**一次性票据**。
 *
 * <p>票据的语义只有一句："刚刚有一个持有该设备私钥的人，在一次新鲜挑战上通过了验证。" 它不是身份、不携带用户信息、不能跨用途使用，有效期 120 秒且用后即删。
 */
public class HumanVerifyResponse {

    /** 一次性票据（业务接口把它原样带上）。 */
    private String humanToken;

    /** 票据有效期（秒）。 */
    private Integer expiresIn;

    /** 本次判定落在哪一档（审计与客户端展示用）：low / mid / high。 */
    private String riskLevel;

    public String getHumanToken() {
        return humanToken;
    }

    public void setHumanToken(String humanToken) {
        this.humanToken = humanToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}

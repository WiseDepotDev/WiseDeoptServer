package com.huicang.wise.application.human;

/**
 * 挑战的应答。
 *
 * <p>{@code action} 只有两种取值（**刻意不做第三档"弹图形码"** —— 图形验证码已整体删除）：
 *
 * <ul>
 *   <li>{@code verify}：正常路径，客户端采集证据、算 {@code difficultyBits} 位的计算量证明后提交；
 *   <li>{@code cooldown}：风险偏高，客户端等 {@code retryAfterMs} 之后**重新申请挑战**再试一次
 *       （这一下的等待与再次点击本身就是交互成本）。第二次仍不达标才拒绝。
 * </ul>
 */
public class HumanChallengeResponse {

    /** 一次性挑战 id。 */
    private String challengeId;

    /** verify 或 cooldown。 */
    private String action;

    /** 需要命中的 SHA-256 前导零 bit 数（0 表示不做计算量证明）。 */
    private Integer difficultyBits;

    /** action=cooldown 时的等待毫秒数。 */
    private Long retryAfterMs;

    /** 挑战有效期（秒）。 */
    private Integer expiresIn;

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getDifficultyBits() {
        return difficultyBits;
    }

    public void setDifficultyBits(Integer difficultyBits) {
        this.difficultyBits = difficultyBits;
    }

    public Long getRetryAfterMs() {
        return retryAfterMs;
    }

    public void setRetryAfterMs(Long retryAfterMs) {
        this.retryAfterMs = retryAfterMs;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }
}

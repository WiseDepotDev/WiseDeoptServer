package com.huicang.wise.application.human;

/**
 * 申请一次人机验证挑战。
 *
 * <p>{@code username} 只在 {@code purpose=LOGIN} 时有用：登录前要按**账号**的失败历史参与风险判定 （连续失败多次 ⇒
 * 提高难度）。它不是凭据，服务端只用它查失败计数。
 */
public class HumanChallengeRequest {

    /** 用途（{@link HumanPurpose}）。缺失或认不出 ⇒ 拒绝。 */
    private String purpose;

    /** 登录用途下的账号（可选；其它用途忽略）。 */
    private String username;

    /** desktop / mobile（与引导文件一致）。 */
    private String platform;

    /** 客户端版本，仅用于审计与"按版本收紧"的将来。 */
    private String clientVersion;

    /**
     * 本地环境证据（可空）。
     *
     * <p><b>为什么证据要在"申请挑战"时一起交</b>：难度是由挑战签发的，而难度必须能反映证据里的
     * 异常（R5/R6）。如果证据只在提交时才到，服务端要么事后改难度（协议变复杂），要么只能 "先发低难度、再要求高难度"（客户端白算一次）。所以客户端**先采集、再申请挑战**。
     */
    private HumanEvidence evidence;

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public HumanEvidence getEvidence() {
        return evidence;
    }

    public void setEvidence(HumanEvidence evidence) {
        this.evidence = evidence;
    }
}

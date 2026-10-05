package com.huicang.wise.application.human;

/**
 * 提交人机验证结果。
 *
 * <p><b>签名的原文</b>（三端必须逐字一致）：
 *
 * <pre>{@code challengeId + "|" + purpose + "|" + powNonce + "|" + deviceKeyId}</pre>
 *
 * <p>为什么 {@code evidence} **不进签名原文**：签名需要跨语言一致的"规范化字符串"，而 JSON 规范化（键序、数字格式、Unicode 转义）在 Kotlin /
 * TypeScript / Java 三处各写一份就是 一个 bug 农场。证据只影响"难度档位"，不参与放行判据，所以不签它 —— 少了它，
 * 攻击者也只能得到"难度低一点"，而仍然需要一把设备私钥 + 一次新鲜挑战。
 */
public class HumanVerifyRequest {

    /** 挑战 id（一次性）。 */
    private String challengeId;

    /** 用途，必须与挑战签发时一致。 */
    private String purpose;

    /** 本地环境证据（可空：空证据只会被判成"交互不足"，不会因此放行）。 */
    private HumanEvidence evidence;

    /** 计算量证明的 nonce（难度为 0 时可空）。 */
    private String powNonce;

    /** 设备公钥指纹（hex）。 */
    private String deviceKeyId;

    /** 设备公钥（未压缩点，130 个 hex 字符）。 */
    private String devicePublicKey;

    /** 设备对签名原文的 ECDSA-SHA256 签名（base64）。 */
    private String signature;

    /** 客户端时间（毫秒）。与服务器差得太多 ⇒ 判为环境可疑。 */
    private Long clientTime;

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public HumanEvidence getEvidence() {
        return evidence;
    }

    public void setEvidence(HumanEvidence evidence) {
        this.evidence = evidence;
    }

    public String getPowNonce() {
        return powNonce;
    }

    public void setPowNonce(String powNonce) {
        this.powNonce = powNonce;
    }

    public String getDeviceKeyId() {
        return deviceKeyId;
    }

    public void setDeviceKeyId(String deviceKeyId) {
        this.deviceKeyId = deviceKeyId;
    }

    public String getDevicePublicKey() {
        return devicePublicKey;
    }

    public void setDevicePublicKey(String devicePublicKey) {
        this.devicePublicKey = devicePublicKey;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getClientTime() {
        return clientTime;
    }

    public void setClientTime(Long clientTime) {
        this.clientTime = clientTime;
    }
}

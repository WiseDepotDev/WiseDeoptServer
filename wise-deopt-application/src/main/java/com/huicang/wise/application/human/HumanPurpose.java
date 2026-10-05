package com.huicang.wise.application.human;

/**
 * 人机验证票据的**用途**。
 *
 * <p>为什么票据要绑用途：票据是"刚刚有一个真人通过了人机验证"的凭证，而不同动作的后果不同 —— 登录失败只是登录失败，删用户是不可逆的。若不绑用途，一次登录验证换来的票据就能拿去删用户
 * （"一个票据干所有事"是这类设计最常见的洞）。所以 {@code purpose} 必须逐个匹配。
 */
public enum HumanPurpose {
    /** 登录（未认证，需要账号维度参与风险判定）。 */
    LOGIN,
    /** 标签批量绑定（破坏性：一次影响多件物料）。 */
    TAG_BATCH_BIND,
    /** 删除用户（不可逆）。 */
    USER_DELETE;

    /** 宽松解析：认不出的用途返回 {@code null}，由调用方拒绝（不猜、不默认放行）。 */
    public static HumanPurpose parse(String text) {
        if (text == null) {
            return null;
        }
        for (HumanPurpose value : values()) {
            if (value.name().equalsIgnoreCase(text.trim())) {
                return value;
            }
        }
        return null;
    }
}

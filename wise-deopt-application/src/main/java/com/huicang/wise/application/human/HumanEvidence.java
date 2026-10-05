package com.huicang.wise.application.human;

/**
 * 客户端上报的**本地环境证据**。
 *
 * <p><b>它只是"证据"，不是"结论"。</b>服务端可以完全不采信：这里每个字段都是客户端能伪造的， 放行判据只能是"签名 + 挑战 + 风险规则 + 计算量证明"。这些字段的作用是 ①
 * 让服务端把难度调高（提高自动化成本）② 给审计留线索。
 *
 * <p>字段刻意保持**扁平与有限**：证据里不许出现任何输入内容（密码、条码值、人名等）， 这是"本地环境验证"与"指纹追踪"的分界线。{@code
 * check-human-verify.mjs} 会按白名单钉住字段名。
 */
public class HumanEvidence {

    /** 证据结构版本。客户端与服务端各留一份，便于以后加字段而不破坏旧客户端。 */
    private Integer version;

    /** desktop / mobile，与引导文件里的 platform 一致。 */
    private String platform;

    /** 壳是否为我们发布的包（Electron 的 app.isPackaged / Android 的签名校验）。 */
    private Boolean shellPackaged;

    /** 是否开着调试通道（Electron 的 remote-debugging / Android 的 Debug.isDebuggerConnected）。 */
    private Boolean debugAttached;

    /** 页面侧：navigator.webdriver 是否为真（自动化框架的默认标志）。 */
    private Boolean webdriver;

    /** 页面侧：无头浏览器特征。 */
    private Boolean headless;

    /** 页面侧：本次点击采集到的指针/触摸采样点数。 */
    private Integer gestureSamples;

    /** 页面侧：按下到抬起的毫秒数。 */
    private Integer gestureDurationMs;

    /**
     * 页面侧：相邻两次指针采样之间的**最大跳变**（像素）。
     *
     * <p>它和"采样点太少"是两件事：脚本可以补一堆采样点（`samples` 很好看），但点与点之间是**瞬移**。 这个字段就是为那种情况留的 ——
     * 页面一直在采它，服务端在此之前**收不到**（字段不在 DTO 里就被 Jackson 丢掉）， 那样"页面采了、服务端看不见"等于白采。
     */
    private Integer maxJumpPx;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Boolean getShellPackaged() {
        return shellPackaged;
    }

    public void setShellPackaged(Boolean shellPackaged) {
        this.shellPackaged = shellPackaged;
    }

    public Boolean getDebugAttached() {
        return debugAttached;
    }

    public void setDebugAttached(Boolean debugAttached) {
        this.debugAttached = debugAttached;
    }

    public Boolean getWebdriver() {
        return webdriver;
    }

    public void setWebdriver(Boolean webdriver) {
        this.webdriver = webdriver;
    }

    public Boolean getHeadless() {
        return headless;
    }

    public void setHeadless(Boolean headless) {
        this.headless = headless;
    }

    public Integer getGestureSamples() {
        return gestureSamples;
    }

    public void setGestureSamples(Integer gestureSamples) {
        this.gestureSamples = gestureSamples;
    }

    public Integer getGestureDurationMs() {
        return gestureDurationMs;
    }

    public void setGestureDurationMs(Integer gestureDurationMs) {
        this.gestureDurationMs = gestureDurationMs;
    }

    public Integer getMaxJumpPx() {
        return maxJumpPx;
    }

    public void setMaxJumpPx(Integer maxJumpPx) {
        this.maxJumpPx = maxJumpPx;
    }
}

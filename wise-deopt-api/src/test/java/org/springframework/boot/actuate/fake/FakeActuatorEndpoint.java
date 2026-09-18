package org.springframework.boot.actuate.fake;

/**
 * 测试替身：仅用于验证 {@code GlobalResponseAdvice.supports()} 的「actuator 不参与信封包装」判据。
 *
 * <p>该判据按**声明类的包名**识别 actuator 端点（真实端点的处理器类位于 {@code
 * org.springframework.boot.actuate.endpoint.web.servlet} 之下）。为了能在单元测试里构造一个 符合该前缀的 {@code
 * MethodParameter}，这里刻意把测试替身放进同名包前缀—— 这是测试专用的命名技巧，不是生产代码。
 */
public class FakeActuatorEndpoint {

    /**
     * 模拟 actuator 端点方法。
     *
     * @return 占位返回值
     */
    public Object handle() {
        return null;
    }
}

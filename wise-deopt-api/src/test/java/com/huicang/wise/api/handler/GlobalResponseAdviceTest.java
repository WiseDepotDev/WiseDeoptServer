package com.huicang.wise.api.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.huicang.wise.api.controller.WarehouseController;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

/**
 * {@link GlobalResponseAdvice#supports} 的判据测试。
 *
 * <p>业务响应统一包成信封是本项目的契约（STD-CONTRACT-01），但**运维端点必须例外**： `/actuator/health` 是容器 HEALTHCHECK、k8s
 * 探针、Prometheus 等按既定 schema 解析的接口， 包一层信封会让它们取不到顶层 `status`。
 *
 * <p>本用例把"哪些来源不参与包装"固定下来，防止后续把 actuator 又包回去。 真实效果的端到端验证见
 * `deploy/smoke-local.ps1`（对运行中的服务直接断言原生格式）。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class GlobalResponseAdviceTest {

    private final GlobalResponseAdvice advice = new GlobalResponseAdvice();

    @Test
    @DisplayName("actuator 端点不应参与信封包装（保持 Spring Boot 原生格式）")
    void shouldNotWrapActuatorEndpoints() throws Exception {
        Method method =
                Class.forName("org.springframework.boot.actuate.fake.FakeActuatorEndpoint")
                        .getMethod("handle");

        boolean supported = advice.supports(new MethodParameter(method, -1), null);

        assertThat(supported).as("actuator 响应被包装会导致监控/探针按原生 schema 解析失败").isFalse();
    }

    @Test
    @DisplayName("业务控制器应参与信封包装")
    void shouldWrapBusinessControllers() throws Exception {
        Method method = WarehouseController.class.getDeclaredMethods()[0];

        boolean supported = advice.supports(new MethodParameter(method, -1), null);

        assertThat(supported).as("业务接口必须返回统一信封").isTrue();
    }

    @Test
    @DisplayName("springdoc / swagger 来源不参与包装")
    void shouldNotWrapSpringdocAndSwagger() throws Exception {
        Method springdocMethod = FakespringdocHandler.class.getDeclaredMethod("handle");
        Method swaggerMethod = FakeswaggerHandler.class.getDeclaredMethod("handle");

        assertThat(advice.supports(new MethodParameter(springdocMethod, -1), null)).isFalse();
        assertThat(advice.supports(new MethodParameter(swaggerMethod, -1), null)).isFalse();
    }

    /**
     * 测试替身：类名中包含小写 `springdoc`，用于触发既有的排除分支。
     *
     * <p>注意名称是 FakespringdocHandler 而非 FakeSpringdocHandler——判据是 {@code
     * className.contains("springdoc")}（大小写敏感），类名必须以小写片段命中。
     */
    static class FakespringdocHandler {
        public Object handle() {
            return null;
        }
    }

    /** 测试替身：类名中包含小写 `swagger`，用于触发既有的排除分支。 */
    static class FakeswaggerHandler {
        public Object handle() {
            return null;
        }
    }
}

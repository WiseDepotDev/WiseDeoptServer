package com.huicang.wise.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.huicang.wise.domain.alert.AlertLevel;
import com.huicang.wise.domain.alert.AlertStatus;
import com.huicang.wise.domain.device.DeviceStatus;
import com.huicang.wise.domain.device.DeviceType;
import com.huicang.wise.domain.inout.StockOrderStatus;
import com.huicang.wise.domain.inout.StockOrderType;
import com.huicang.wise.domain.inspection.InspectionStatus;
import com.huicang.wise.domain.inspection.InspectionType;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 带编码枚举的契约测试。
 *
 * <p>这些枚举的 `code` 会落库并与设备端/APP 交互，因此把「编码唯一、可往返、未知编码返回 null、
 * 描述非空」固化为可执行约束：任何一处编码改动或重复，都会在这里失败，而不是等到线上出现 无法解释的状态值。
 *
 * <p>说明：本轮**刻意不为每个枚举单独建测试类**——每个枚举只有 `code`/`description` 两个字段与一个 `fromCode` 循环，拆成 9 个类只会得到 9
 * 份几乎相同的浅测试（coverage theater）。用一张表覆盖 全部编码枚举的**共同契约**，价值与信号密度都更高。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class DomainEnumCodeContractTest {

    /**
     * 需要满足「编码契约」的枚举清单。
     *
     * <p>新增带编码的枚举时必须加到这里——这是清单式约束，不是自动发现，因此遗漏会被人工评审拦住。
     */
    private static final List<Class<? extends Enum<?>>> CODED_ENUMS =
            List.of(
                    AlertLevel.class,
                    AlertStatus.class,
                    DeviceStatus.class,
                    DeviceType.class,
                    StockOrderStatus.class,
                    StockOrderType.class,
                    InspectionStatus.class,
                    InspectionType.class);

    @Test
    @DisplayName("每个带编码枚举都应能用自己的编码往返还原为同一常量")
    void shouldRoundTripEveryConstantByItsCode() throws Exception {
        for (Class<? extends Enum<?>> type : CODED_ENUMS) {
            for (Enum<?> constant : type.getEnumConstants()) {
                Integer code = codeOf(constant);
                assertThat(code)
                        .as("%s.%s 的 code 不应为 null", type.getSimpleName(), constant.name())
                        .isNotNull();

                Object restored = fromCode(type, code);
                assertThat(restored)
                        .as("%s.fromCode(%d) 应还原为 %s", type.getSimpleName(), code, constant.name())
                        .isEqualTo(constant);
            }
        }
    }

    @Test
    @DisplayName("同一枚举内的编码不允许重复")
    void shouldNotHaveDuplicateCodes() {
        for (Class<? extends Enum<?>> type : CODED_ENUMS) {
            Set<Integer> seen = new HashSet<>();
            for (Enum<?> constant : type.getEnumConstants()) {
                Integer code = codeOf(constant);
                assertThat(seen.add(code))
                        .as("%s 中编码 %d 重复出现（%s）", type.getSimpleName(), code, constant.name())
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("未知编码应返回 null 而不是抛异常或猜一个默认值")
    void shouldReturnNullWhenCodeIsUnknown() throws Exception {
        for (Class<? extends Enum<?>> type : CODED_ENUMS) {
            int unknown = maxCode(type) + 1000;
            assertThat(fromCode(type, unknown))
                    .as("%s.fromCode(%d) 应返回 null", type.getSimpleName(), unknown)
                    .isNull();
        }
    }

    @Test
    @DisplayName("编码为 null 时应返回 null 而不是默认值")
    void shouldReturnNullWhenCodeIsNull() throws Exception {
        for (Class<? extends Enum<?>> type : CODED_ENUMS) {
            assertThat(fromCode(type, null))
                    .as("%s.fromCode(null) 应返回 null", type.getSimpleName())
                    .isNull();
        }
    }

    @Test
    @DisplayName("每个常量的描述都应为非空文本")
    void shouldHaveNonBlankDescriptionForEveryConstant() throws Exception {
        for (Class<? extends Enum<?>> type : CODED_ENUMS) {
            for (Enum<?> constant : type.getEnumConstants()) {
                String description =
                        (String) constant.getClass().getMethod("getDescription").invoke(constant);
                assertThat(description)
                        .as("%s.%s 的 description 不应为空", type.getSimpleName(), constant.name())
                        .isNotBlank();
            }
        }
    }

    private static Integer codeOf(Enum<?> constant) {
        try {
            return (Integer) constant.getClass().getMethod("getCode").invoke(constant);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "枚举缺少 public Integer getCode()：" + constant.getClass().getName(), e);
        }
    }

    private static Object fromCode(Class<? extends Enum<?>> type, Integer code) throws Exception {
        Method method = type.getMethod("fromCode", Integer.class);
        return method.invoke(null, code);
    }

    private static int maxCode(Class<? extends Enum<?>> type) {
        int max = Integer.MIN_VALUE;
        for (Enum<?> constant : type.getEnumConstants()) {
            max = Math.max(max, codeOf(constant));
        }
        return max;
    }
}

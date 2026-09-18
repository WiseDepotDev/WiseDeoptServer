package com.huicang.wise.domain.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link Language} 的行为测试。
 *
 * <p>`Language` 与其它编码枚举的关键差异：编码是 String，且 `fromCode` **对未知/空值回退到 {@link Language#ZH_CN} 而不是返回
 * null**。这是一个对外可见的默认语义（影响响应语言）， 必须被固定下来，避免后续把回退行为改成返回 null 造成空指针或语言错乱。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class LanguageTest {

    @Test
    @DisplayName("每个语言都能按自身编码还原")
    void shouldRoundTripEveryLanguageByItsCode() {
        for (Language language : Language.values()) {
            assertThat(Language.fromCode(language.getCode())).isEqualTo(language);
        }
    }

    @Test
    @DisplayName("未知编码应回退到简体中文而不是返回 null")
    void shouldFallBackToSimplifiedChineseWhenCodeIsUnknown() {
        assertThat(Language.fromCode("de-DE")).isEqualTo(Language.ZH_CN);
        assertThat(Language.fromCode("")).isEqualTo(Language.ZH_CN);
    }

    @Test
    @DisplayName("编码为 null 时应回退到简体中文而不是抛异常")
    void shouldFallBackToSimplifiedChineseWhenCodeIsNull() {
        assertThat(Language.fromCode(null)).isEqualTo(Language.ZH_CN);
    }

    @Test
    @DisplayName("语言编码必须唯一且非空")
    void shouldHaveUniqueNonBlankCodes() {
        Set<String> seen = new HashSet<>();
        for (Language language : Language.values()) {
            assertThat(language.getCode()).as("%s 的 code 不应为空", language.name()).isNotBlank();
            assertThat(seen.add(language.getCode())).as("编码 %s 重复", language.getCode()).isTrue();
        }
    }

    @Test
    @DisplayName("每个语言的显示名必须非空")
    void shouldHaveNonBlankDisplayName() {
        for (Language language : Language.values()) {
            assertThat(language.getDisplayName())
                    .as("%s 的 displayName", language.name())
                    .isNotBlank();
        }
    }
}

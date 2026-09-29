package com.huicang.wise.application.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * 国际化服务的单元测试：内置词条、按 Locale / 按语言码两种查询、缺失兜底，以及词条增改与读取。
 *
 * <p>本批钉住三处现状（只记录、未修）： ① {@code getTranslations("zh-CN")} 返回的是**内部活 Map**，调用方可以直接改掉服务内部状态（无防御性拷贝）；
 * ② {@code getTranslations("xx")} 每次返回一个**全新且脱离内部状态的空 Map**，往里面写是**静默丢失**的； ③ 语言码传 {@code null} 会抛
 * **NPE**（{@code ConcurrentHashMap.get(null)}），Loclet 传 null 亦然； 另外**兜底只兜到 zh-CN**，某个 key 只有 en-US
 * 有、用 zh-CN 查时不会跨语言兜底，而是直接返回 key 本身。
 */
@ExtendWith(MockitoExtension.class)
class I18nServiceTest {

    private static final String KEY = "common.success";

    @Mock private AcceptHeaderLocaleResolver localeResolver;

    private I18nService service;

    @BeforeEach
    void setUp() {
        service = new I18nService(localeResolver);
    }

    // ---------------- 内置词条 ----------------

    @Test
    @DisplayName("初始化：内置 zh-CN 与 en-US 两套词条，各 14 条")
    void seedsTwoLanguages() {
        assertEquals(14, service.getTranslations("zh-CN").size());
        assertEquals(14, service.getTranslations("en-US").size());
        assertEquals("操作成功", service.getTranslations("zh-CN").get(KEY));
        assertEquals("Success", service.getTranslations("en-US").get(KEY));
    }

    @Test
    @DisplayName("按 Locale 查询：zh-CN 命中中文、en-US 命中英文")
    void translateByLocale() {
        assertEquals("操作成功", service.translate(KEY, Locale.forLanguageTag("zh-CN")));
        assertEquals("Success", service.translate(KEY, Locale.US));
        // Locale.ENGLISH 的语言标签是 "en"（不是 "en-US"），未收录 ⇒ 兜底中文
        assertEquals("取消", service.translate("common.cancel", Locale.ENGLISH));
    }

    @Test
    @DisplayName("按 Locale 查询：未收录的语言兜底到 zh-CN")
    void unknownLocaleFallsBackToChinese() {
        assertEquals("操作成功", service.translate(KEY, Locale.JAPAN));
        assertEquals("导出", service.translate("common.export", Locale.GERMANY));
    }

    @Test
    @DisplayName("按 Locale 查询：key 不存在时原样返回 key")
    void unknownKeyReturnsKeyItself() {
        assertEquals("no.such.key", service.translate("no.such.key", Locale.US));
        assertEquals("no.such.key", service.translate("no.such.key", Locale.JAPAN));
    }

    @Test
    @DisplayName("按语言码查询：命中与兜底行为与 Locale 版一致")
    void translateByLanguageCode() {
        assertEquals("Success", service.translate(KEY, "en-US"));
        assertEquals("操作成功", service.translate(KEY, "zh-CN"));
        assertEquals("操作成功", service.translate(KEY, "fr-FR"));
        assertEquals("no.such.key", service.translate("no.such.key", "en-US"));
    }

    @Test
    @DisplayName("兜底只兜到 zh-CN：只有 en-US 有的 key 用 zh-CN 查会原样返回 key")
    void fallbackIsNotCrossLanguage() {
        service.addTranslation("en-US", "only.en", "English only");

        assertEquals("English only", service.translate("only.en", "en-US"));
        assertEquals(
                "only.en", service.translate("only.en", "zh-CN"), "现状：兜底只在 zh-CN 里找，不会去 en-US 找");
    }

    @Test
    @DisplayName("无参查询：通过 localeResolver 解析，且传入的是 null 请求")
    void translateWithoutLocaleUsesResolver() {
        when(localeResolver.resolveLocale(null)).thenReturn(Locale.US);

        assertEquals("Success", service.translate(KEY));

        verify(localeResolver).resolveLocale(null);
    }

    // ---------------- 词条增改 ----------------

    @Test
    @DisplayName("新增词条：对已存在语言写入新 key")
    void addTranslationToExistingLanguage() {
        service.addTranslation("zh-CN", "custom.key", "自定义");

        assertEquals("自定义", service.translate("custom.key", "zh-CN"));
        assertEquals(15, service.getTranslations("zh-CN").size());
    }

    @Test
    @DisplayName("新增词条：可以新建一门语言（并可被查询命中）")
    void addTranslationCreatesNewLanguage() {
        service.addTranslation("ja-JP", KEY, "成功しました");

        assertEquals("成功しました", service.translate(KEY, "ja-JP"));
        assertEquals("操作成功", service.translate(KEY, "zh-CN"), "原有语言不受影响");
    }

    @Test
    @DisplayName("新增词条：同 key 重复写入即覆盖")
    void addTranslationOverwrites() {
        service.addTranslation("zh-CN", KEY, "改过了");

        assertEquals("改过了", service.translate(KEY, "zh-CN"));
    }

    // ---------------- 现状缺陷 ----------------

    @Test
    @DisplayName("现状缺陷①：getTranslations 返回内部活 Map，调用方可直接改掉服务状态")
    void getTranslationsExposesInternalMap() {
        Map<String, String> zh = service.getTranslations("zh-CN");
        zh.put(KEY, "被外部改掉了");

        assertEquals("被外部改掉了", service.translate(KEY, "zh-CN"), "现状：没有防御性拷贝，外部可直接污染内部词条");
    }

    @Test
    @DisplayName("现状缺陷②：查未收录语言返回的是脱离内部状态的空 Map，写入会静默丢失")
    void getTranslationsForUnknownLanguageIsDetached() {
        Map<String, String> fresh = service.getTranslations("ko-KR");
        assertTrue(fresh.isEmpty());

        fresh.put("common.success", "성공");

        assertTrue(service.getTranslations("ko-KR").isEmpty(), "现状：写入的是临时 Map，服务内部并不存在 ko-KR");
        assertEquals("操作成功", service.translate(KEY, "ko-KR"), "查询仍然兜底到 zh-CN");
    }

    @Test
    @DisplayName("现状缺陷③：语言码为 null 抛 NPE（ConcurrentHashMap.get(null)）")
    void nullLanguageCodeThrowsNpe() {
        assertThrows(NullPointerException.class, () -> service.translate(KEY, (String) null));
    }

    @Test
    @DisplayName("现状缺陷③：Locale 为 null 抛 NPE（locale.toLanguageTag()）")
    void nullLocaleThrowsNpe() {
        assertThrows(NullPointerException.class, () -> service.translate(KEY, (Locale) null));
    }

    @Test
    @DisplayName("getTranslations：内置语言的返回对象在多次调用间是同一个（同一引用）")
    void getTranslationsReturnsSameInstanceForSeededLanguage() {
        assertSame(service.getTranslations("en-US"), service.getTranslations("en-US"));
    }

    @Test
    @DisplayName("Locale 标签匹配：zh_CN 与 en_US 都能按语言标签命中")
    void localeLanguageTagMatching() {
        assertEquals("操作成功", service.translate(KEY, new Locale("zh", "CN")));
        assertEquals("Success", service.translate(KEY, new Locale("en", "US")));
    }
}

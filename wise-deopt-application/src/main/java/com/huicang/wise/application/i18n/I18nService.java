package com.huicang.wise.application.i18n;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Service
public class I18nService {

    private final AcceptHeaderLocaleResolver localeResolver;
    private final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();

    public I18nService(AcceptHeaderLocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
        initializeTranslations();
    }

    private void initializeTranslations() {
        Map<String, String> zhCN = new ConcurrentHashMap<>();
        zhCN.put("common.success", "操作成功");
        zhCN.put("common.error", "操作失败");
        zhCN.put("common.loading", "加载中...");
        zhCN.put("common.confirm", "确认");
        zhCN.put("common.cancel", "取消");
        zhCN.put("common.delete", "删除");
        zhCN.put("common.edit", "编辑");
        zhCN.put("common.save", "保存");
        zhCN.put("common.back", "返回");
        zhCN.put("common.refresh", "刷新");
        zhCN.put("common.search", "搜索");
        zhCN.put("common.filter", "筛选");
        zhCN.put("common.export", "导出");
        zhCN.put("common.import", "导入");

        Map<String, String> enUS = new ConcurrentHashMap<>();
        enUS.put("common.success", "Success");
        enUS.put("common.error", "Error");
        enUS.put("common.loading", "Loading...");
        enUS.put("common.confirm", "Confirm");
        enUS.put("common.cancel", "Cancel");
        enUS.put("common.delete", "Delete");
        enUS.put("common.edit", "Edit");
        enUS.put("common.save", "Save");
        enUS.put("common.back", "Back");
        enUS.put("common.refresh", "Refresh");
        enUS.put("common.search", "Search");
        enUS.put("common.filter", "Filter");
        enUS.put("common.export", "Export");
        enUS.put("common.import", "Import");

        translations.put("zh-CN", zhCN);
        translations.put("en-US", enUS);
    }

    public String translate(String key, Locale locale) {
        String languageCode = locale.toLanguageTag();
        Map<String, String> languageTranslations = translations.get(languageCode);

        if (languageTranslations != null && languageTranslations.containsKey(key)) {
            return languageTranslations.get(key);
        }

        Map<String, String> defaultTranslations = translations.get("zh-CN");
        if (defaultTranslations != null && defaultTranslations.containsKey(key)) {
            return defaultTranslations.get(key);
        }

        return key;
    }

    public String translate(String key, String languageCode) {
        Map<String, String> languageTranslations = translations.get(languageCode);

        if (languageTranslations != null && languageTranslations.containsKey(key)) {
            return languageTranslations.get(key);
        }

        Map<String, String> defaultTranslations = translations.get("zh-CN");
        if (defaultTranslations != null && defaultTranslations.containsKey(key)) {
            return defaultTranslations.get(key);
        }

        return key;
    }

    public String translate(String key) {
        Locale locale = localeResolver.resolveLocale(null);
        return translate(key, locale);
    }

    public void addTranslation(String languageCode, String key, String value) {
        translations.computeIfAbsent(languageCode, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    public Map<String, String> getTranslations(String languageCode) {
        return translations.getOrDefault(languageCode, new ConcurrentHashMap<>());
    }
}

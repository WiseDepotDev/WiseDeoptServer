package com.huicang.wise.api.controller;

import com.huicang.wise.application.i18n.I18nService;
import com.huicang.wise.common.api.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/i18n")
public class I18nController {

    @Autowired private I18nService i18nService;

    @GetMapping("/translations")
    public ApiResponse<Map<String, String>> getTranslations(
            @RequestParam(value = "lang", defaultValue = "zh-CN") String languageCode) {
        Map<String, String> translations = i18nService.getTranslations(languageCode);
        return ApiResponse.success(translations);
    }

    @GetMapping("/translate")
    public ApiResponse<String> translate(
            @RequestParam("key") String key,
            @RequestParam(value = "lang", defaultValue = "zh-CN") String languageCode) {
        String translation = i18nService.translate(key, languageCode);
        return ApiResponse.success(translation);
    }

    @GetMapping("/languages")
    public ApiResponse<com.huicang.wise.domain.i18n.Language[]> getSupportedLanguages() {
        com.huicang.wise.domain.i18n.Language[] languages =
                com.huicang.wise.domain.i18n.Language.values();
        return ApiResponse.success(languages);
    }
}

package com.huicang.wise.domain.i18n;

public enum Language {
    ZH_CN("zh-CN", "简体中文"),
    ZH_TW("zh-TW", "繁體中文"),
    EN_US("en-US", "English"),
    JA_JP("ja-JP", "日本語"),
    KO_KR("ko-KR", "한국어");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.code.equals(code)) {
                return language;
            }
        }
        return ZH_CN;
    }
}
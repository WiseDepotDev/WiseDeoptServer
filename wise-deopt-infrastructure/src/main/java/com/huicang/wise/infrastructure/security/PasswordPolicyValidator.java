package com.huicang.wise.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");

    public ValidationResult validatePassword(String password) {
        ValidationResult result = new ValidationResult();

        if (password == null || password.isEmpty()) {
            result.addError("密码不能为空");
            return result;
        }

        if (password.length() < MIN_LENGTH) {
            result.addError("密码长度不能少于" + MIN_LENGTH + "位");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            result.addError("密码必须包含小写字母");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            result.addError("密码必须包含大写字母");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            result.addError("密码必须包含数字");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            result.addError("密码必须包含特殊字符");
        }

        return result;
    }

    public int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int strength = 0;

        if (password.length() >= MIN_LENGTH) {
            strength += 1;
        }
        if (password.length() >= 12) {
            strength += 1;
        }

        if (LOWERCASE_PATTERN.matcher(password).find()) {
            strength += 1;
        }
        if (UPPERCASE_PATTERN.matcher(password).find()) {
            strength += 1;
        }
        if (DIGIT_PATTERN.matcher(password).find()) {
            strength += 1;
        }
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            strength += 1;
        }

        return Math.min(strength, 5);
    }

    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return errors;
        }
    }
}

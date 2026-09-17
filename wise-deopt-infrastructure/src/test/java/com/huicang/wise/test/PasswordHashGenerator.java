package com.huicang.wise.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成工具
 * 用于生成测试用户的BCrypt密码哈希
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        // 生成测试密码的哈希值
        String adminPassword = "admin123";
        String operatorPassword = "operator123";
        String visitorPassword = "visitor123";

        String adminHash = encoder.encode(adminPassword);
        String operatorHash = encoder.encode(operatorPassword);
        String visitorHash = encoder.encode(visitorPassword);

        System.out.println("=================================");
        System.out.println("BCrypt Password Hashes (cost=12)");
        System.out.println("=================================\n");

        System.out.println("Admin User:");
        System.out.println("  Username: admin");
        System.out.println("  Password: " + adminPassword);
        System.out.println("  Hash: " + adminHash);
        System.out.println();

        System.out.println("Operator User:");
        System.out.println("  Username: operator");
        System.out.println("  Password: " + operatorPassword);
        System.out.println("  Hash: " + operatorHash);
        System.out.println();

        System.out.println("Visitor User:");
        System.out.println("  Username: visitor");
        System.out.println("  Password: " + visitorPassword);
        System.out.println("  Hash: " + visitorHash);
        System.out.println();

        // 验证生成的哈希
        System.out.println("Verification:");
        System.out.println("============");
        System.out.println("Verify admin123: " + encoder.matches(adminPassword, adminHash));
        System.out.println("Verify operator123: " + encoder.matches(operatorPassword, operatorHash));
        System.out.println("Verify visitor123: " + encoder.matches(visitorPassword, visitorHash));
    }
}

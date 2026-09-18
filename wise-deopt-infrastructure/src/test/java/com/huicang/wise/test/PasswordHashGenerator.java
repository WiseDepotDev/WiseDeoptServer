package com.huicang.wise.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成工具。
 *
 * <p>用于为测试/初始化数据生成 BCrypt 密码哈希。源码中不得出现真实口令（STD-SEC-01）， 需要真实口令时通过命令行参数传入：
 *
 * <pre>java PasswordHashGenerator &lt;password&gt; [&lt;password&gt; ...]</pre>
 *
 * <p>不带参数运行时使用示例占位口令，仅供本地验证算法可用性。
 */
public class PasswordHashGenerator {

    /** 示例占位口令，非任何环境的真实口令。 */
    private static final String[] EXAMPLE_PASSWORDS = {
        "Example-Admin-Passw0rd", "Example-Operator-Passw0rd", "Example-Visitor-Passw0rd"
    };

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String[] passwords = args.length > 0 ? args : EXAMPLE_PASSWORDS;

        System.out.println("=================================");
        System.out.println("BCrypt Password Hashes (cost=12)");
        System.out.println("=================================");
        if (args.length == 0) {
            System.out.println("（未传入口令参数，使用示例占位口令）");
        }
        System.out.println();

        for (int i = 0; i < passwords.length; i++) {
            String password = passwords[i];
            String hash = encoder.encode(password);
            System.out.println("Entry " + (i + 1) + ":");
            System.out.println("  Hash: " + hash);
            System.out.println("  Verify: " + encoder.matches(password, hash));
            System.out.println();
        }
    }
}

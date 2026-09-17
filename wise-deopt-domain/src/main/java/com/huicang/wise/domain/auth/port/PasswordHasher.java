package com.huicang.wise.domain.auth.port;

/**
 * 口令散列端口（领域端口 / Port）。
 *
 * <p>存在意义：入口层（api）不得直接依赖 infrastructure（STD-ARCH-02/05），
 * 而在启动初始化等场景需要生成口令散列。故在领域层定义端口，
 * 由 {@code com.huicang.wise.infrastructure.security.PasswordEncoder} 提供适配实现。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
public interface PasswordHasher {

    /**
     * 生成口令散列。
     *
     * @param rawPassword 原始口令
     * @return 散列结果
     */
    String encode(String rawPassword);

    /**
     * 校验口令与散列是否匹配。
     *
     * @param rawPassword     原始口令
     * @param encodedPassword 已存散列
     * @return true 表示匹配
     */
    boolean matches(String rawPassword, String encodedPassword);
}

package com.huicang.wise.domain.auth.port;

/**
 * 访问令牌校验端口（领域端口 / Port）。
 *
 * <p>存在意义：按 STD-ARCH-02/05，入口层（api）**不得直接依赖 infrastructure**。
 * 令牌校验属于技术实现，故在领域层定义端口，由基础设施层提供适配实现
 * （{@code com.huicang.wise.infrastructure.security.JwtTokenProvider}）。
 *
 * <p>本端口只声明**校验与解析**能力（入口层需要的能力）；令牌签发等仅应用层使用的能力
 * 保留在具体实现上，待后续统一迁移。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
public interface TokenVerifier {

    /**
     * 校验令牌是否有效（签名正确且未过期）。
     *
     * @param token 令牌字符串
     * @return true 表示有效
     */
    boolean validateToken(String token);

    /**
     * 从令牌中解析用户名。
     *
     * @param token 令牌字符串
     * @return 用户名；解析失败返回 null
     */
    String getUsernameFromToken(String token);

    /**
     * 从令牌中解析用户主键。
     *
     * @param token 令牌字符串
     * @return 用户主键；解析失败返回 null
     */
    Long getUserIdFromToken(String token);
}

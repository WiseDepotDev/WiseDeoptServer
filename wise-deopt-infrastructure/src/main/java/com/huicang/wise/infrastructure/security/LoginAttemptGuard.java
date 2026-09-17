package com.huicang.wise.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/**
 * 类功能描述：登录失败节流与账户锁定
 * <p>
 * 修复背景：项目原本只有 {@code user_login_log} 落库，<b>没有任何失败计数与锁定逻辑</b>
 * （{@code ErrorCode.AUTH_ACCOUNT_LOCKED} 从未被使用），
 * 攻击者可在验证码通过后无限次尝试口令。
 * <p>
 * 同时原实现把失败次数放在实例内存的 {@code ConcurrentHashMap}（并在方法结束时清理），
 * 多实例部署下各自计数、限流形同虚设。本类改用 Redis：
 * <ul>
 *     <li>跨实例一致，重启不清零</li>
 *     <li>使用 Lua 脚本原子执行"读计数 + 判断"，避免并发竞态</li>
 *     <li>key 命名遵循《服务端架构规划》中的 {@code auth:login:fail:{userId}}</li>
 * </ul>
 *
 * <p><b>可用性取舍</b>：Redis 不可用时采取 fail-open（放行并打 ERROR 日志），
 * 避免因缓存故障导致全体用户无法登录；生产环境应对 Redis 做高可用。
 *
 * @author WiseDepot
 * @since 2026-09-17
 */
@Component
@Slf4j
public class LoginAttemptGuard {

    private static final String FAIL_KEY_PREFIX = "auth:login:fail:user:";
    private static final String FAIL_IP_KEY_PREFIX = "auth:login:fail:ip:";

    /**
     * 原子操作：读取当前失败次数；若已达上限返回 1（锁定），否则返回 0（放行）。
     * 不在这里自增，自增由 {@link #onFailure} 负责，这样可以精确区分
     * "验证码失败"（不计数）与"口令失败"（计数）。
     */
    private static final DefaultRedisScript<Long> IS_LOCKED_SCRIPT;

    static {
        IS_LOCKED_SCRIPT = new DefaultRedisScript<>();
        IS_LOCKED_SCRIPT.setScriptText(
                "local current = redis.call('get', KEYS[1])\n" +
                "if current and tonumber(current) >= tonumber(ARGV[1]) then\n" +
                "    return 1\n" +
                "else\n" +
                "    return 0\n" +
                "end"
        );
        IS_LOCKED_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    /** 触发锁定的失败次数阈值 */
    private final int maxFailures;

    /** 计数与锁定的时间窗口（秒） */
    private final int windowSeconds;

    /** 同一 IP 的失败次数阈值（防止对大量不同账号撞库） */
    private final int maxFailuresPerIp;

    public LoginAttemptGuard(
            StringRedisTemplate stringRedisTemplate,
            @Value("${wise.login-attempt.max-failures:5}") int maxFailures,
            @Value("${wise.login-attempt.window-seconds:900}") int windowSeconds,
            @Value("${wise.login-attempt.max-failures-per-ip:50}") int maxFailuresPerIp) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.maxFailures = maxFailures;
        this.windowSeconds = windowSeconds;
        this.maxFailuresPerIp = maxFailuresPerIp;
    }

    /**
     * 方法功能描述：判断该账号是否已被锁定
     *
     * @param username 登录名
     * @return true 表示已锁定
     */
    public boolean isLocked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        try {
            Long r = stringRedisTemplate.execute(
                    IS_LOCKED_SCRIPT,
                    Collections.singletonList(FAIL_KEY_PREFIX + username),
                    String.valueOf(maxFailures));
            return r != null && r == 1L;
        } catch (Exception e) {
            log.error("读取登录失败次数失败(fail-open 放行): {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 方法功能描述：判断该 IP 是否已触发撞库保护
     *
     * @param ip 客户端 IP
     * @return true 表示已封禁
     */
    public boolean isIpBlocked(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            Long r = stringRedisTemplate.execute(
                    IS_LOCKED_SCRIPT,
                    Collections.singletonList(FAIL_IP_KEY_PREFIX + ip),
                    String.valueOf(maxFailuresPerIp));
            return r != null && r == 1L;
        } catch (Exception e) {
            log.error("读取 IP 失败次数失败(fail-open 放行): {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 方法功能描述：记录一次认证失败（口令/PIN 错误时才应调用）
     *
     * @param username 登录名
     * @param ip       客户端 IP
     * @return 本次失败后该账号的失败次数
     */
    public long onFailure(String username, String ip) {
        long count = 0;
        if (username != null && !username.isBlank()) {
            count = increment(FAIL_KEY_PREFIX + username);
            if (count == maxFailures) {
                log.warn("账号 {} 连续登录失败 {} 次，已锁定 {} 秒", username, count, windowSeconds);
            }
        }
        if (ip != null && !ip.isBlank()) {
            long ipCount = increment(FAIL_IP_KEY_PREFIX + ip);
            if (ipCount == maxFailuresPerIp) {
                log.warn("IP {} 登录失败累计 {} 次，已封禁 {} 秒", ip, ipCount, windowSeconds);
            }
        }
        return count;
    }

    /**
     * 方法功能描述：登录成功后清零该账号的失败计数（不清理 IP 计数，避免被用来洗白撞库）
     *
     * @param username 登录名
     */
    public void onSuccess(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.delete(FAIL_KEY_PREFIX + username);
        } catch (Exception e) {
            log.error("清理登录失败计数失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 方法功能描述：查询当前失败次数（仅用于日志/测试）
     */
    public int currentFailures(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        try {
            String v = stringRedisTemplate.opsForValue().get(FAIL_KEY_PREFIX + username);
            return v == null ? 0 : Integer.parseInt(v);
        } catch (Exception e) {
            log.error("查询登录失败次数失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    public int getMaxFailures() {
        return maxFailures;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    private long increment(String key) {
        try {
            Long v = stringRedisTemplate.opsForValue().increment(key);
            if (v != null && v == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return v == null ? 0L : v;
        } catch (Exception e) {
            log.error("累加登录失败次数失败: {}", e.getMessage(), e);
            return 0L;
        }
    }
}

package com.huicang.wise.application.auth;

import com.huicang.wise.application.human.HumanPurpose;
import com.huicang.wise.application.human.HumanVerifyApplicationService;
import com.huicang.wise.application.oss.FileStorageApplicationService;
import com.huicang.wise.application.password.PasswordApplicationService;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.UserLoginLog;
import com.huicang.wise.domain.user.NfcBadge;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.domain.user.UserProfile;
import com.huicang.wise.domain.user.UserSecurity;
import com.huicang.wise.infrastructure.persistence.repository.auth.PermissionRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.RolePermissionRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.RoleRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.UserLoginLogRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.UserRoleRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.NfcBadgeRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserCoreRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserProfileRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserSecurityRepository;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import com.huicang.wise.infrastructure.security.JwtTokenProvider;
import com.huicang.wise.infrastructure.security.LoginAttemptGuard;
import com.huicang.wise.infrastructure.security.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthApplicationService {

    private final UserCoreRepository userCoreRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserLoginLogRepository userLoginLogRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final UserProfileRepository userProfileRepository;
    private final NfcBadgeRepository nfcBadgeRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordApplicationService passwordApplicationService;
    private final FileStorageApplicationService fileStorageApplicationService;
    private final HumanVerifyApplicationService humanVerifyApplicationService;
    private final LoginAttemptGuard loginAttemptGuard;

    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{4,6}$");

    /**
     * 已吊销令牌的键前缀（吊销集）。
     *
     * <p><b>为什么需要它</b>：登录时会把令牌写进 `auth:token:access:<用户名>`，登出时把那条键删掉 —— 但 `validateToken`
     * **从来不读**它，只验 JWT 签名与用户状态。于是"登出"只是客户端把令牌丢掉了： 任何一个之前被复制走的令牌（同机日志、抓包、另一台设备）仍然能用到它自己过期为止（access 2
     * 小时）。 写进去没人读的键不是"接入了 Redis"，只是一个看起来很像缓存的摆设。
     *
     * <p><b>为什么不用那个 per-user 键做校验</b>：`auth:token:access:<用户名>` 每个用户**只存最后一张**，
     * 拿它当白名单会让"手机登录把桌面踢下线"成为默认行为 —— 那是产品决定，不是安全修复。 吊销集只回答一个问题："这一张令牌被明确作废过吗"，多端登录互不影响。
     *
     * <p><b>为什么存指纹而不是令牌原文</b>：Redis 的备份/快照会落到磁盘上，存原文等于把可用凭据 复制到另一个地方；指纹（SHA-256 前 32
     * hex）足够判定"是不是同一张"，且不可反推。
     */
    private static final String REVOKED_PREFIX = "auth:revoked:token:";

    /** 吊销记录的兜底存活期（秒）：解析不到 JWT 过期时间时用，宁长勿短。 */
    private static final long REVOKED_FALLBACK_SECONDS = 7 * 24 * 3600L;

    public AuthApplicationService(
            UserCoreRepository userCoreRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            PermissionRepository permissionRepository,
            UserRoleRepository userRoleRepository,
            UserLoginLogRepository userLoginLogRepository,
            UserSecurityRepository userSecurityRepository,
            UserProfileRepository userProfileRepository,
            NfcBadgeRepository nfcBadgeRepository,
            StringRedisTemplate stringRedisTemplate,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            PasswordApplicationService passwordApplicationService,
            FileStorageApplicationService fileStorageApplicationService,
            HumanVerifyApplicationService humanVerifyApplicationService,
            LoginAttemptGuard loginAttemptGuard) {
        this.userCoreRepository = userCoreRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userLoginLogRepository = userLoginLogRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.userProfileRepository = userProfileRepository;
        this.nfcBadgeRepository = nfcBadgeRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordApplicationService = passwordApplicationService;
        this.fileStorageApplicationService = fileStorageApplicationService;
        this.humanVerifyApplicationService = humanVerifyApplicationService;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest)
            throws BusinessException {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(
                    ErrorCode.VAL_PARAM_AUTH_USERNAME_EMPTY,
                    ErrorCode.VAL_PARAM_AUTH_USERNAME_EMPTY.getMessage());
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(
                    ErrorCode.VAL_PARAM_AUTH_PASSWORD_EMPTY,
                    ErrorCode.VAL_PARAM_AUTH_PASSWORD_EMPTY.getMessage());
        }

        /*
         * 人机验证票据为必填项。
         *
         * **历史教训原样保留在这里**：修复前用的是
         * `if (captchaId != null && captchaCode != null) { 校验 }` —— 攻击者不传那两个字段
         * 就能跳过校验、无限次爆破口令。所以这里的写法只能有一种：**缺失即失败**，
         * 由 `HumanVerifyApplicationService.enforce` 抛 HUMAN_TOKEN_REQUIRED。
         */
        humanVerifyApplicationService.enforce(request.getHumanToken(), HumanPurpose.LOGIN);

        // 登录失败节流：验证码通过后仍要防"换口令重试"。
        // 计数存 Redis，多实例一致；命中即直接拒绝，不再查询数据库。
        String clientIp = getClientIp(httpServletRequest);
        if (loginAttemptGuard.isLocked(request.getUsername())) {
            recordLoginAttempt(null, "PASSWORD", httpServletRequest, "FAILED", "账号已锁定");
            log.warn("账号 {} 已被锁定，拒绝登录（IP: {}）", request.getUsername(), clientIp);
            throw new BusinessException(
                    ErrorCode.AUTH_ACCOUNT_LOCKED, ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage());
        }
        if (loginAttemptGuard.isIpBlocked(clientIp)) {
            recordLoginAttempt(null, "PASSWORD", httpServletRequest, "FAILED", "IP已封禁");
            log.warn("IP {} 登录失败次数过多，已封禁", clientIp);
            throw new BusinessException(
                    ErrorCode.AUTH_ACCOUNT_LOCKED, ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage());
        }

        UserCore user = userCoreRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null) {
            // 用户不存在也计入失败，避免用"账号是否存在"的差异做枚举
            loginAttemptGuard.onFailure(request.getUsername(), clientIp);
            recordLoginAttempt(null, "PASSWORD", httpServletRequest, "FAILED", "用户不存在");
            throw new BusinessException(
                    ErrorCode.AUTH_REQUEST_USERNAME_PASSWORD_ERROR,
                    ErrorCode.AUTH_REQUEST_USERNAME_PASSWORD_ERROR.getMessage());
        }

        checkAccountStatus(user);

        UserSecurity userSecurity =
                userSecurityRepository
                        .findByUserId(user.getUserId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户安全信息不存在"));

        if (!passwordEncoder.matches(request.getPassword(), userSecurity.getPasswordHash())) {
            handleLoginFailure(user, userSecurity, "PASSWORD", httpServletRequest, "密码错误");
            throw new BusinessException(
                    ErrorCode.AUTH_REQUEST_USERNAME_PASSWORD_ERROR,
                    ErrorCode.AUTH_REQUEST_USERNAME_PASSWORD_ERROR.getMessage());
        }

        handleLoginSuccess(user, userSecurity, "PASSWORD", httpServletRequest);
        return generateTokens(user, httpServletRequest);
    }

    public NfcLoginResponse loginNfc(
            UserNfcLoginDTO request, HttpServletRequest httpServletRequest) {
        NfcBadge nfcBadge =
                nfcBadgeRepository
                        .findByNfcUid(request.getNfcId())
                        .orElseThrow(
                                () -> {
                                    recordLoginAttempt(
                                            null, "NFC", httpServletRequest, "FAILED", "NFC未找到");
                                    return new BusinessException(
                                            ErrorCode.AUTH_REQUEST_NFC_NOT_FOUND,
                                            ErrorCode.AUTH_REQUEST_NFC_NOT_FOUND.getMessage());
                                });

        UserCore user =
                userCoreRepository
                        .findById(nfcBadge.getUserId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        checkAccountStatus(user);

        UserProfile userProfile =
                userProfileRepository
                        .findByUserId(user.getUserId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户资料不存在"));

        NfcLoginResponse response = new NfcLoginResponse();
        response.setUsername(user.getUsername());
        response.setNickname(userProfile.getNickname());

        if (userProfile.getAvatarFileId() != null) {
            // 返回头像API访问路径
            response.setAvatarFileUrl("/api/profile/" + user.getUserId() + "/avatar/image");
        } else {
            response.setAvatarFileUrl(null);
        }

        response.setVerificationId(UUID.randomUUID().toString());
        return response;
    }

    public LoginResponse nfcPinLogin(NfcPinDTO request, HttpServletRequest httpServletRequest) {
        if (request.getPin() == null || request.getPin().isBlank()) {
            throw new BusinessException(ErrorCode.VAL_PARAM_AUTH_PIN_EMPTY, "PIN码不能为空");
        }

        if (!PIN_PATTERN.matcher(request.getPin()).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "PIN码必须是4-6位数字");
        }

        NfcBadge nfcBadge =
                nfcBadgeRepository
                        .findByNfcUid(request.getNfcId())
                        .orElseThrow(
                                () -> {
                                    recordLoginAttempt(
                                            null,
                                            "NFC_PIN",
                                            httpServletRequest,
                                            "FAILED",
                                            "NFC未找到");
                                    return new BusinessException(
                                            ErrorCode.AUTH_REQUEST_NFC_NOT_FOUND,
                                            ErrorCode.AUTH_REQUEST_NFC_NOT_FOUND.getMessage());
                                });

        UserCore user =
                userCoreRepository
                        .findById(nfcBadge.getUserId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        checkAccountStatus(user);

        // 该端点免验证码，且 PIN 仅 4-6 位数字（约 111 万种组合），必须做失败节流
        String nfcClientIp = getClientIp(httpServletRequest);
        if (loginAttemptGuard.isLocked(user.getUsername())) {
            recordLoginAttempt(user.getUserId(), "NFC_PIN", httpServletRequest, "FAILED", "账号已锁定");
            log.warn("账号 {} 已被锁定，拒绝 NFC+PIN 登录（IP: {}）", user.getUsername(), nfcClientIp);
            throw new BusinessException(
                    ErrorCode.AUTH_ACCOUNT_LOCKED, ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage());
        }
        if (loginAttemptGuard.isIpBlocked(nfcClientIp)) {
            recordLoginAttempt(null, "NFC_PIN", httpServletRequest, "FAILED", "IP已封禁");
            log.warn("IP {} 登录失败次数过多，已封禁", nfcClientIp);
            throw new BusinessException(
                    ErrorCode.AUTH_ACCOUNT_LOCKED, ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage());
        }

        if (nfcBadge.getPinHash() == null
                || !passwordEncoder.matches(request.getPin(), nfcBadge.getPinHash())) {
            handleLoginFailure(user, null, "NFC_PIN", httpServletRequest, "PIN码错误");
            throw new BusinessException(
                    ErrorCode.AUTH_REQUEST_PIN_ERROR,
                    ErrorCode.AUTH_REQUEST_PIN_ERROR.getMessage());
        }

        handleLoginSuccess(user, null, "NFC_PIN", httpServletRequest);
        return generateTokens(user, httpServletRequest);
    }

    public LoginResponse refreshToken(
            RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "刷新令牌无效或已过期");
        }

        if (!jwtTokenProvider.isTokenType(refreshToken, "refresh")) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "令牌类型错误");
        }

        // 登出时连刷新令牌一起吊销 —— 否则"退出了"但还能用 refreshToken 换一对新的回来
        if (isRevoked(refreshToken)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "令牌已登出");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        UserCore user =
                userCoreRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        checkAccountStatus(user);

        return generateTokens(user, httpServletRequest);
    }

    public String validateToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "无效的访问令牌");
        }

        if (!jwtTokenProvider.isTokenType(token, "access")) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "令牌类型错误");
        }

        // 已登出的令牌必须当场失效（否则 logout 只是"客户端忘了它"，服务端仍然认）
        if (isRevoked(token)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "令牌已登出");
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);

        UserCore user =
                userCoreRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(
                    ErrorCode.AUTH_ACCOUNT_DISABLED, ErrorCode.AUTH_ACCOUNT_DISABLED.getMessage());
        }

        return username;
    }

    /**
     * 权限检查（每个需要 `@RequiresPermission` 的请求都会走这里）。
     *
     * <p><b>为什么返回 `boolean` 而不是 `void`</b>：`@Cacheable` 的切面只缓存**非 null 的返回值** （见
     * `RedisCacheAspect`：`if (result != null && …)`）。原来是 `void` ⇒ 每读一次缓存都是未命中，
     * 每执行一次又什么都存不下，于是这个注解**看着像缓存、实际每请求查 4 张表** （用户 → 角色 → 角色权限 → 权限）。改成返回 `true` 之后"通过"才会真的被缓存 15
     * 分钟。
     *
     * <p>拒绝（抛 `FORBIDDEN`）不进缓存：异常路径本来就不缓存，而拒绝是少数路径， 让它每次都真实查库也符合"权限收紧必须立即生效"的直觉。
     */
    @Cacheable(prefix = "auth:permission", key = "#username + ':' + #permissionCode", timeout = 900)
    public boolean checkPermission(String username, String permissionCode) {
        UserCore user =
                userCoreRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        List<com.huicang.wise.domain.auth.UserRole> userRoles =
                userRoleRepository.findByUserId(user.getUserId());
        if (userRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户无角色");
        }

        for (com.huicang.wise.domain.auth.UserRole userRole : userRoles) {
            Long roleId = userRole.getRoleId();
            com.huicang.wise.domain.auth.Role role = roleRepository.findById(roleId).orElse(null);

            if (role != null) {
                String roleName = role.getName();
                log.debug("检查用户 {} 的角色: {}, 角色ID: {}", username, roleName, roleId);

                if ("超级管理员".equals(roleName)
                        || "管理员".equals(roleName)
                        || "ADMIN".equalsIgnoreCase(roleName)) {
                    log.debug("用户 {} 拥有管理员角色 {}，跳过权限检查", username, roleName);
                    return true;
                }
            }
        }

        List<Long> roleIds =
                userRoles.stream().map(com.huicang.wise.domain.auth.UserRole::getRoleId).toList();

        List<com.huicang.wise.domain.auth.RolePermission> rolePermissions =
                rolePermissionRepository.findByRoleIdIn(roleIds);
        List<Long> permissionIds =
                rolePermissions.stream()
                        .map(com.huicang.wise.domain.auth.RolePermission::getPermissionId)
                        .toList();

        List<com.huicang.wise.domain.auth.Permission> permissions =
                permissionRepository.findAllById(permissionIds);
        boolean hasPermission =
                permissions.stream().anyMatch(p -> p.getCode().equals(permissionCode));

        log.debug("用户 {} 检查权限 {}: {}", username, permissionCode, hasPermission ? "通过" : "失败");

        if (!hasPermission) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此资源");
        }
        return true;
    }

    public void logout(String token) {
        if (token != null) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String accessKey = "auth:token:access:" + username;
            String refreshKey = "auth:token:refresh:" + username;
            // 先把刷新令牌读出来一起吊销，再删键 —— 反过来的话就读不到了（键已删）
            String refreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
            if (refreshToken != null) {
                revoke(refreshToken);
            }
            stringRedisTemplate.delete(accessKey);
            stringRedisTemplate.delete(refreshKey);
            // 记进吊销集：**这一张**令牌立即失效，而不是等它自己过期（access 默认 2 小时）。
            // TTL 取"它本来还能活多久"，过期即自动清理 —— 吊销集不会无限增长。
            revoke(token);
            log.info("用户 {} 退出登录（访问令牌与刷新令牌均已吊销）", username);
        }
    }

    /** 这张令牌被明确作废过吗。 */
    private boolean isRevoked(String token) {
        try {
            return Boolean.TRUE.equals(
                    stringRedisTemplate.hasKey(REVOKED_PREFIX + tokenFingerprint(token)));
        } catch (Exception e) {
            // Redis 不可用：**不放行也不误杀**是矛盾的，这里选择"认为没吊销"（保持登录可用），
            // 与 LoginAttemptGuard/限流一致的取舍 —— Redis 是硬依赖，启动时就要求可连，
            // 运行期偶发失败不该把所有人踢下线。
            log.error("查询令牌吊销状态失败，本次按未吊销处理: {}", e.getMessage());
            return false;
        }
    }

    /** 把令牌记进吊销集，TTL = 它的剩余有效期（解析不到就用兜底值）。 */
    private void revoke(String token) {
        try {
            long ttlSeconds = REVOKED_FALLBACK_SECONDS;
            Date expiry = jwtTokenProvider.getExpirationDateFromToken(token);
            if (expiry != null) {
                long remaining = (expiry.getTime() - System.currentTimeMillis()) / 1000;
                if (remaining > 0) {
                    ttlSeconds = remaining;
                }
            }
            stringRedisTemplate
                    .opsForValue()
                    .set(
                            REVOKED_PREFIX + tokenFingerprint(token),
                            "1",
                            Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.error("吊销令牌失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 令牌指纹：SHA-256 的前 32 个 hex 字符。
     *
     * <p>与设备指纹同一套写法（见 `DeviceKeyStore` / `HumanVerifyApplicationService`）： 够长、够短、不可反推，且**不把凭据原文写进
     * Redis**。
     */
    private static String tokenFingerprint(String token) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private void checkAccountStatus(UserCore user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(
                    ErrorCode.AUTH_ACCOUNT_DISABLED, ErrorCode.AUTH_ACCOUNT_DISABLED.getMessage());
        }
    }

    private void handleLoginSuccess(
            UserCore user,
            UserSecurity userSecurity,
            String loginType,
            HttpServletRequest httpServletRequest) {
        // 登录成功清零该账号的失败计数（IP 计数不清，避免被用来洗白撞库）
        loginAttemptGuard.onSuccess(user.getUsername());
        recordLoginAttempt(user.getUserId(), loginType, httpServletRequest, "SUCCESS", null);
        log.info("用户 {} 登录成功，登录类型: {}", user.getUsername(), loginType);
    }

    private void handleLoginFailure(
            UserCore user,
            UserSecurity userSecurity,
            String loginType,
            HttpServletRequest httpServletRequest,
            String failureReason) {
        // 口令/PIN 错误才计数，验证码错误不计入（验证码本身就是一次性防重放）
        long failures =
                loginAttemptGuard.onFailure(user.getUsername(), getClientIp(httpServletRequest));
        recordLoginAttempt(
                user.getUserId(), loginType, httpServletRequest, "FAILED", failureReason);
        log.warn(
                "用户 {} 登录失败，失败原因: {}（累计失败 {} 次，阈值 {}）",
                user.getUsername(),
                failureReason,
                failures,
                loginAttemptGuard.getMaxFailures());
    }

    private void recordLoginAttempt(
            Long userId,
            String loginType,
            HttpServletRequest httpServletRequest,
            String status,
            String failureReason) {
        try {
            UserLoginLog log = new UserLoginLog();
            log.setUserId(userId);
            log.setLoginType(convertLoginType(loginType));
            log.setIpAddress(getClientIp(httpServletRequest));
            log.setUserAgent(httpServletRequest.getHeader("User-Agent"));
            log.setSuccess(status.equals("SUCCESS") ? (short) 1 : (short) 0);
            log.setFailureReason(failureReason);
            log.setLoginTime(LocalDateTime.now());
            userLoginLogRepository.save(log);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    private Short convertLoginType(String loginType) {
        if (loginType == null) {
            return (short) 0;
        }
        switch (loginType) {
            case "NFC":
            case "NFC_PIN":
                return 3;
            default:
                return 0;
        }
    }

    private LoginResponse generateTokens(UserCore user, HttpServletRequest httpServletRequest) {
        String accessToken =
                jwtTokenProvider.generateAccessToken(user.getUsername(), user.getUserId());
        String refreshToken =
                jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getUserId());

        String accessKey = "auth:token:access:" + user.getUsername();
        String refreshKey = "auth:token:refresh:" + user.getUsername();

        try {
            stringRedisTemplate.opsForValue().set(accessKey, accessToken, Duration.ofHours(1));
            stringRedisTemplate.opsForValue().set(refreshKey, refreshToken, Duration.ofDays(7));
        } catch (Exception e) {
            log.error("Redis存储token失败: {}", e.getMessage(), e);
        }

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUsername(user.getUsername());
        response.setPasswordChangeRequired(false);
        return response;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

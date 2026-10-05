package com.huicang.wise.application.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.huicang.wise.application.human.HumanPurpose;
import com.huicang.wise.application.human.HumanVerifyApplicationService;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.user.NfcBadge;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.infrastructure.persistence.repository.user.NfcBadgeRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserCoreRepository;
import com.huicang.wise.infrastructure.security.JwtTokenProvider;
import com.huicang.wise.infrastructure.security.LoginAttemptGuard;
import com.huicang.wise.infrastructure.security.PasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock private UserCoreRepository userCoreRepository;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    @Mock private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @Mock private HumanVerifyApplicationService humanVerifyApplicationService;

    @Mock private LoginAttemptGuard loginAttemptGuard;

    @Mock private NfcBadgeRepository nfcBadgeRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private AuthApplicationService authApplicationService;

    @BeforeEach
    void setUp() {
        // leniency allows stubbing to be ignored if not used (e.g. in exception tests)
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testNfcPinLoginSuccess() {
        String nfcId = "test-nfc";
        String pin = "123456";
        String pinHash = "$2a$mock$hash";

        UserCore user = new UserCore();
        user.setUsername("testuser");
        user.setUserId(1L);
        user.setStatus((short) 1);

        NfcBadge badge = new NfcBadge();
        badge.setNfcUid(nfcId);
        badge.setUserId(1L);
        badge.setPinHash(pinHash);

        when(nfcBadgeRepository.findByNfcUid(nfcId)).thenReturn(Optional.of(badge));
        when(userCoreRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(pin, pinHash)).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken("testuser", 1L)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("testuser", 1L)).thenReturn("refresh-token");

        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId(nfcId);
        request.setPin(pin);

        LoginResponse response = authApplicationService.nfcPinLogin(request, httpServletRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertNotNull(response.getAccessToken());

        // 登录成功应清零失败计数
        verify(loginAttemptGuard, times(1)).onSuccess("testuser");
        verify(loginAttemptGuard, never()).onFailure(anyString(), any());
    }

    @Test
    void testNfcPinLoginWrongPin() {
        String nfcId = "test-nfc";
        String pin = "123456";
        String wrongPin = "654321";
        String pinHash = "$2a$mock$hash";

        UserCore user = new UserCore();
        user.setUsername("testuser");
        user.setUserId(1L);
        user.setStatus((short) 1);

        NfcBadge badge = new NfcBadge();
        badge.setNfcUid(nfcId);
        badge.setUserId(1L);
        badge.setPinHash(pinHash);

        when(nfcBadgeRepository.findByNfcUid(nfcId)).thenReturn(Optional.of(badge));
        when(userCoreRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongPin, pinHash)).thenReturn(false);

        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId(nfcId);
        request.setPin(wrongPin);

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.nfcPinLogin(request, httpServletRequest));

        // PIN 错误必须累加失败计数
        verify(loginAttemptGuard, times(1)).onFailure(eq("testuser"), any());
        verify(loginAttemptGuard, never()).onSuccess(anyString());
    }

    @Test
    void testNfcPinLoginLockout() {
        String nfcId = "test-nfc";
        String wrongPin = "654321";
        String pinHash = "$2a$mock$hash";

        UserCore user = new UserCore();
        user.setUsername("testuser");
        user.setUserId(1L);
        user.setStatus((short) 1);

        NfcBadge badge = new NfcBadge();
        badge.setNfcUid(nfcId);
        badge.setUserId(1L);
        badge.setPinHash(pinHash);

        when(nfcBadgeRepository.findByNfcUid(nfcId)).thenReturn(Optional.of(badge));
        when(userCoreRepository.findById(1L)).thenReturn(Optional.of(user));
        // 账号已被锁定
        when(loginAttemptGuard.isLocked("testuser")).thenReturn(true);

        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId(nfcId);
        request.setPin(wrongPin);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> authApplicationService.nfcPinLogin(request, httpServletRequest));

        assertEquals(com.huicang.wise.common.api.ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());
        // 锁定时不应再校验 PIN，也不应改变计数
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(loginAttemptGuard, never()).onFailure(anyString(), any());
    }

    @Test
    void testNfcPinLoginAccountLocked() {
        String nfcId = "test-nfc";
        String pin = "123456";

        UserCore user = new UserCore();
        user.setUserId(1L);
        user.setUsername("test-user");
        user.setStatus((short) 1);

        NfcBadge badge = new NfcBadge();
        badge.setNfcUid(nfcId);
        badge.setUserId(1L);
        badge.setPinHash("$2a$mock$hash");

        when(nfcBadgeRepository.findByNfcUid(nfcId)).thenReturn(Optional.of(badge));
        when(userCoreRepository.findById(1L)).thenReturn(Optional.of(user));
        when(loginAttemptGuard.isIpBlocked(any())).thenReturn(true);

        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId(nfcId);
        request.setPin(pin);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> authApplicationService.nfcPinLogin(request, httpServletRequest));

        // IP 已封禁 -> 直接拒绝，不应校验 PIN
        assertEquals(com.huicang.wise.common.api.ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    // ==================================================================
    // 防回归：人机验证票据不可绕过
    //
    // 历史（图形验证码时代）的缺陷代码：
    //     if (request.getCaptchaId() != null && request.getCaptchaCode() != null) { 校验 }
    // 攻击者只要不传这两个字段，验证码校验就被整段跳过，可无限次爆破口令。
    // 现在统一走 humanVerifyApplicationService.enforce(...)，**缺失即抛异常**。
    // 这几条用例存在的唯一目的：让"缺字段就跳过"这种写法再也回不来。
    // ==================================================================

    /** 不传票据时，必须在进入任何用户查询之前就被拦截 */
    @Test
    void testLoginWithoutHumanTokenIsRejected() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Test-Admin-Passw0rd");
        // humanToken 为 null

        doThrow(
                        new BusinessException(
                                com.huicang.wise.common.api.ErrorCode.HUMAN_TOKEN_REQUIRED,
                                "缺少人机验证票据"))
                .when(humanVerifyApplicationService)
                .enforce(null, HumanPurpose.LOGIN);

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        // 关键断言：未通过人机验证时绝不能触碰用户数据（即不可能被用来爆破口令）
        verify(userCoreRepository, never()).findByUsername(anyString());
        verify(humanVerifyApplicationService, times(1)).enforce(null, HumanPurpose.LOGIN);
    }

    /** 票据无效（过期/已用/伪造）同样不得触碰用户数据 */
    @Test
    void testLoginWithInvalidTokenIsRejected() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Test-Admin-Passw0rd");
        request.setHumanToken("forged-ticket");

        doThrow(
                        new BusinessException(
                                com.huicang.wise.common.api.ErrorCode.HUMAN_TOKEN_INVALID,
                                "人机验证票据无效或已使用"))
                .when(humanVerifyApplicationService)
                .enforce("forged-ticket", HumanPurpose.LOGIN);

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(userCoreRepository, never()).findByUsername(anyString());
        verify(humanVerifyApplicationService, times(1))
                .enforce("forged-ticket", HumanPurpose.LOGIN);
    }

    /** 票据正确时校验必须真的被执行（防止有人把它改成空实现） */
    @Test
    void testLoginInvokesHumanVerification() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Test-Admin-Passw0rd");
        request.setHumanToken("ticket-2");

        // 校验通过（不抛异常），后续因用户不存在而失败，足以证明校验被调用
        doNothing().when(humanVerifyApplicationService).enforce("ticket-2", HumanPurpose.LOGIN);
        when(userCoreRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(humanVerifyApplicationService, times(1)).enforce("ticket-2", HumanPurpose.LOGIN);
    }

    // ==================================================================
    // 防回归：登录失败节流 / 账户锁定
    //
    // 修复前：只有 user_login_log 落库，没有任何失败计数与锁定，
    // 且项目文档里的 auth:login:fail:{userId} 方案完全没实现。
    // 修复后：计数存 Redis（LoginAttemptGuard），跨实例一致；
    // 人机验证通过后仍能拦住"换口令重试"。
    // ==================================================================

    private LoginRequest buildLoginRequest(String username, String password) {
        LoginRequest r = new LoginRequest();
        r.setUsername(username);
        r.setPassword(password);
        r.setHumanToken("ticket-lock");
        return r;
    }

    /** 账号被锁定时必须直接拒绝，且不得查询数据库 */
    @Test
    void testLoginRejectedWhenAccountLocked() {
        LoginRequest request = buildLoginRequest("admin", "Test-Admin-Passw0rd");
        doNothing().when(humanVerifyApplicationService).enforce("ticket-lock", HumanPurpose.LOGIN);
        when(loginAttemptGuard.isLocked("admin")).thenReturn(true);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> authApplicationService.login(request, httpServletRequest));

        assertEquals(com.huicang.wise.common.api.ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());
        // 锁定后不应触碰用户数据
        verify(userCoreRepository, never()).findByUsername(anyString());
        verify(loginAttemptGuard, never()).onSuccess(anyString());
    }

    /** IP 被封禁时同样拒绝 */
    @Test
    void testLoginRejectedWhenIpBlocked() {
        LoginRequest request = buildLoginRequest("admin", "Test-Admin-Passw0rd");
        doNothing().when(humanVerifyApplicationService).enforce("ticket-lock", HumanPurpose.LOGIN);
        when(loginAttemptGuard.isLocked("admin")).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(any())).thenReturn(true);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> authApplicationService.login(request, httpServletRequest));

        assertEquals(com.huicang.wise.common.api.ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());
        verify(userCoreRepository, never()).findByUsername(anyString());
    }

    /** 用户不存在也必须计入失败，避免账号枚举差异 */
    @Test
    void testLoginUnknownUserCountsFailure() {
        LoginRequest request = buildLoginRequest("ghost", "whatever");
        doNothing().when(humanVerifyApplicationService).enforce("ticket-lock", HumanPurpose.LOGIN);
        when(loginAttemptGuard.isLocked("ghost")).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(any())).thenReturn(false);
        when(userCoreRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(loginAttemptGuard, times(1)).onFailure(eq("ghost"), any());
    }

    /** 人机验证未通过时不应累加失败计数（票据本身是一次性的，撞不出口令） */
    @Test
    void testHumanVerifyFailureDoesNotCountAttempt() {
        LoginRequest request = buildLoginRequest("admin", "Test-Admin-Passw0rd");
        doThrow(
                        new BusinessException(
                                com.huicang.wise.common.api.ErrorCode.HUMAN_TOKEN_INVALID,
                                "人机验证票据无效或已使用"))
                .when(humanVerifyApplicationService)
                .enforce("ticket-lock", HumanPurpose.LOGIN);

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(loginAttemptGuard, never()).onFailure(anyString(), any());
    }
}

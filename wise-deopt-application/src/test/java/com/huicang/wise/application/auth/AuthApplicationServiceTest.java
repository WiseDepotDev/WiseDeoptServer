package com.huicang.wise.application.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.huicang.wise.application.captcha.CaptchaApplicationService;
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

    @Mock private CaptchaApplicationService captchaApplicationService;

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
    // 防回归：验证码不可绕过
    //
    // 修复前的缺陷代码：
    //     if (request.getCaptchaId() != null && request.getCaptchaCode() != null) { 校验 }
    // 攻击者只要不传这两个字段，验证码校验就被整段跳过，可无限次爆破口令。
    // 修复后统一走 captchaApplicationService.enforceCaptcha(...)，缺失即抛异常。
    // ==================================================================

    /** 不传验证码字段时，必须在进入任何用户查询之前就被拦截 */
    @Test
    void testLoginWithoutCaptchaIdIsRejected() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Test-Admin-Passw0rd");
        // captchaId / captchaCode 均为 null

        doThrow(
                        new BusinessException(
                                com.huicang.wise.common.api.ErrorCode
                                        .VAL_PARAM_AUTH_CAPTCHA_ID_EMPTY,
                                "captchaId字段为空"))
                .when(captchaApplicationService)
                .enforceCaptcha(null, null);

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        // 关键断言：验证码未通过时绝不能触碰用户数据（即不可能被用来爆破口令）
        verify(userCoreRepository, never()).findByUsername(anyString());
        verify(captchaApplicationService, times(1)).enforceCaptcha(null, null);
    }

    /** 只传 captchaId 不传 captchaCode 也必须被拦截 */
    @Test
    void testLoginWithCaptchaIdButNoCodeIsRejected() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Test-Admin-Passw0rd");
        request.setCaptchaId("some-captcha-id");
        // captchaCode 仍为 null

        doThrow(
                        new BusinessException(
                                com.huicang.wise.common.api.ErrorCode
                                        .VAL_PARAM_AUTH_CAPTCHA_CODE_EMPTY,
                                "captchaCode字段为空"))
                .when(captchaApplicationService)
                .enforceCaptcha("some-captcha-id", null);

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(userCoreRepository, never()).findByUsername(anyString());
    }

    /** 验证码错误时同样不得触碰用户数据 */
    @Test
    void testLoginWithWrongCaptchaIsRejected() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Test-Admin-Passw0rd");
        request.setCaptchaId("cid-1");
        request.setCaptchaCode("ZZZZ");

        doThrow(new BusinessException(com.huicang.wise.common.api.ErrorCode.PARAM_ERROR, "验证码错误"))
                .when(captchaApplicationService)
                .enforceCaptcha("cid-1", "ZZZZ");

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(userCoreRepository, never()).findByUsername(anyString());
    }

    /** 验证码正确时校验必须真的被执行（防止把校验改成空实现） */
    @Test
    void testLoginInvokesCaptchaVerification() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Test-Admin-Passw0rd");
        request.setCaptchaId("cid-2");
        request.setCaptchaCode("AB12");

        // 校验通过（不抛异常），后续因用户不存在而失败，足以证明校验被调用
        doNothing().when(captchaApplicationService).enforceCaptcha("cid-2", "AB12");
        when(userCoreRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(captchaApplicationService, times(1)).enforceCaptcha("cid-2", "AB12");
    }

    // ==================================================================
    // 防回归：登录失败节流 / 账户锁定
    //
    // 修复前：只有 user_login_log 落库，没有任何失败计数与锁定，
    // 且项目文档里的 auth:login:fail:{userId} 方案完全没实现。
    // 修复后：计数存 Redis（LoginAttemptGuard），跨实例一致；
    // 验证码通过后仍能拦住"换口令重试"。
    // ==================================================================

    private LoginRequest buildLoginRequest(String username, String password) {
        LoginRequest r = new LoginRequest();
        r.setUsername(username);
        r.setPassword(password);
        r.setCaptchaId("cid-lock");
        r.setCaptchaCode("OK12");
        return r;
    }

    /** 账号被锁定时必须直接拒绝，且不得查询数据库 */
    @Test
    void testLoginRejectedWhenAccountLocked() {
        LoginRequest request = buildLoginRequest("admin", "Test-Admin-Passw0rd");
        doNothing().when(captchaApplicationService).enforceCaptcha("cid-lock", "OK12");
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
        doNothing().when(captchaApplicationService).enforceCaptcha("cid-lock", "OK12");
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
        doNothing().when(captchaApplicationService).enforceCaptcha("cid-lock", "OK12");
        when(loginAttemptGuard.isLocked("ghost")).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(any())).thenReturn(false);
        when(userCoreRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(loginAttemptGuard, times(1)).onFailure(eq("ghost"), any());
    }

    /** 验证码校验失败时不应累加失败计数（验证码本身是防重放的） */
    @Test
    void testCaptchaFailureDoesNotCountAttempt() {
        LoginRequest request = buildLoginRequest("admin", "Test-Admin-Passw0rd");
        doThrow(new BusinessException(com.huicang.wise.common.api.ErrorCode.PARAM_ERROR, "验证码错误"))
                .when(captchaApplicationService)
                .enforceCaptcha("cid-lock", "OK12");

        assertThrows(
                BusinessException.class,
                () -> authApplicationService.login(request, httpServletRequest));

        verify(loginAttemptGuard, never()).onFailure(anyString(), any());
    }
}

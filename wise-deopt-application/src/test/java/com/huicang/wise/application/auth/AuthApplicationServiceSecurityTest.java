package com.huicang.wise.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.human.HumanPurpose;
import com.huicang.wise.application.human.HumanVerifyApplicationService;
import com.huicang.wise.application.oss.FileStorageApplicationService;
import com.huicang.wise.application.password.PasswordApplicationService;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.Permission;
import com.huicang.wise.domain.auth.Role;
import com.huicang.wise.domain.auth.RolePermission;
import com.huicang.wise.domain.auth.UserLoginLog;
import com.huicang.wise.domain.auth.UserRole;
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
import com.huicang.wise.infrastructure.security.JwtTokenProvider;
import com.huicang.wise.infrastructure.security.LoginAttemptGuard;
import com.huicang.wise.infrastructure.security.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 认证应用服务的单元测试：口令登录、NFC 登录、NFC+PIN 登录、令牌刷新与校验、权限检查、登出。
 *
 * <p>本类的安全语义是本批重点，逐条钉住： ① **验证码前置**：{@code login} 必须先校验验证码，且验证码不通过时**不得触达任何仓储**（防无限爆破）； ②
 * **节流前置**：账号锁定 / IP 封禁的判定发生在**查库之前**，命中即直接拒绝； ③ **不泄露账号是否存在**：用户不存在与口令错误返回**同一个错误码**，且用户不存在也会计入失败；
 * ④ **NFC+PIN 端点免验证码**，因此必须有失败节流与 4-6 位数字格式校验； ⑤ 令牌类型必须区分 access/refresh（防止用刷新令牌直连接口）。
 */
@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceSecurityTest {

    private static final long USER_ID = 42L;
    private static final String USERNAME = "zhang";
    private static final String IP = "10.1.2.3";

    @Mock private UserCoreRepository userCoreRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserLoginLogRepository userLoginLogRepository;
    @Mock private UserSecurityRepository userSecurityRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private NfcBadgeRepository nfcBadgeRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordApplicationService passwordApplicationService;
    @Mock private FileStorageApplicationService fileStorageApplicationService;
    @Mock private HumanVerifyApplicationService humanVerifyApplicationService;
    @Mock private LoginAttemptGuard loginAttemptGuard;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new AuthApplicationService(
                        userCoreRepository,
                        roleRepository,
                        rolePermissionRepository,
                        permissionRepository,
                        userRoleRepository,
                        userLoginLogRepository,
                        userSecurityRepository,
                        userProfileRepository,
                        nfcBadgeRepository,
                        stringRedisTemplate,
                        passwordEncoder,
                        jwtTokenProvider,
                        passwordApplicationService,
                        fileStorageApplicationService,
                        humanVerifyApplicationService,
                        loginAttemptGuard);
        // `opsForValue()` 是登出/写入路径都要用到的入口，统一在这里给（否则每个用例都要自己 stub）
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private HttpServletRequest http() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(IP);
        lenient().when(request.getHeader("User-Agent")).thenReturn("junit");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        return request;
    }

    private UserCore user(Short status) {
        UserCore entity = new UserCore();
        entity.setUserId(USER_ID);
        entity.setUsername(USERNAME);
        entity.setStatus(status);
        return entity;
    }

    private UserSecurity security() {
        UserSecurity entity = new UserSecurity();
        entity.setPasswordHash("HASH");
        return entity;
    }

    private NfcBadge badge(String pinHash) {
        NfcBadge entity = new NfcBadge();
        entity.setNfcUid("NFC-1");
        entity.setPinHash(pinHash);
        return entity;
    }

    private UserProfile profile(Long avatarFileId) {
        UserProfile entity = new UserProfile();
        entity.setUserId(USER_ID);
        entity.setNickname("张三");
        entity.setAvatarFileId(avatarFileId);
        return entity;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword("secret");
        request.setHumanToken("ticket");
        return request;
    }

    private void stubTokenSuccess() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(jwtTokenProvider.generateAccessToken(USERNAME, USER_ID)).thenReturn("A");
        lenient().when(jwtTokenProvider.generateRefreshToken(USERNAME, USER_ID)).thenReturn("R");
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ---------------- 口令登录 ----------------

    @Test
    @DisplayName("登录：用户名为空抛 VAL_PARAM_AUTH_USERNAME_EMPTY")
    void loginRejectsBlankUsername() {
        LoginRequest request = loginRequest();
        request.setUsername("  ");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(request, http()));

        assertEquals(ErrorCode.VAL_PARAM_AUTH_USERNAME_EMPTY, ex.getErrorCode());
        verifyNoInteractions(humanVerifyApplicationService);
    }

    @Test
    @DisplayName("登录：口令为空抛 VAL_PARAM_AUTH_PASSWORD_EMPTY")
    void loginRejectsBlankPassword() {
        LoginRequest request = loginRequest();
        request.setPassword(null);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(request, http()));

        assertEquals(ErrorCode.VAL_PARAM_AUTH_PASSWORD_EMPTY, ex.getErrorCode());
        verifyNoInteractions(humanVerifyApplicationService);
    }

    @Test
    @DisplayName("登录：人机验证未通过时不得触达任何仓储（防无限爆破）")
    void loginStopsAtHumanVerify() {
        doThrow(new BusinessException(ErrorCode.HUMAN_TOKEN_INVALID, "人机验证票据无效或已使用"))
                .when(humanVerifyApplicationService)
                .enforce("ticket", HumanPurpose.LOGIN);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(loginRequest(), http()));

        assertEquals(ErrorCode.HUMAN_TOKEN_INVALID, ex.getErrorCode());
        verifyNoInteractions(userCoreRepository);
        verifyNoInteractions(userSecurityRepository);
        verifyNoInteractions(loginAttemptGuard);
    }

    @Test
    @DisplayName("登录：账号已锁定直接拒绝并记失败日志")
    void loginRejectsLockedAccount() {
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(loginRequest(), http()));

        assertEquals(ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());
        ArgumentCaptor<UserLoginLog> captor = ArgumentCaptor.forClass(UserLoginLog.class);
        verify(userLoginLogRepository).save(captor.capture());
        assertEquals("账号已锁定", captor.getValue().getFailureReason());
        verify(userCoreRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("登录：IP 被封禁直接拒绝")
    void loginRejectsBlockedIp() {
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(loginRequest(), http()));

        assertEquals(ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());
        verify(userCoreRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("登录：用户不存在与口令错误返回同一错误码，且计入失败")
    void loginHidesWhetherUserExists() {
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(false);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(loginRequest(), http()));

        assertEquals(ErrorCode.AUTH_REQUEST_USERNAME_PASSWORD_ERROR, ex.getErrorCode());
        verify(loginAttemptGuard).onFailure(USERNAME, IP);
    }

    @Test
    @DisplayName("登录：账号被停用时抛 AUTH_ACCOUNT_DISABLED")
    void loginRejectsDisabledAccount() {
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(false);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 0)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(loginRequest(), http()));

        assertEquals(ErrorCode.AUTH_ACCOUNT_DISABLED, ex.getErrorCode());
        verify(userSecurityRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("登录：缺少安全信息行抛 NOT_FOUND")
    void loginWithoutSecurityRow() {
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(false);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(loginRequest(), http()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("登录：口令错误返回同一错误码并累计失败")
    void loginRejectsWrongPassword() {
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(false);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(security()));
        when(passwordEncoder.matches("secret", "HASH")).thenReturn(false);
        when(loginAttemptGuard.onFailure(USERNAME, IP)).thenReturn(1L);
        when(loginAttemptGuard.getMaxFailures()).thenReturn(5);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.login(loginRequest(), http()));

        assertEquals(ErrorCode.AUTH_REQUEST_USERNAME_PASSWORD_ERROR, ex.getErrorCode());
        verify(loginAttemptGuard).onFailure(USERNAME, IP);
        verify(jwtTokenProvider, never()).generateAccessToken(any(), any());
    }

    @Test
    @DisplayName("登录：成功时清零失败计数、写日志、下发并缓存双令牌")
    void loginSuccessIssuesTokens() {
        stubTokenSuccess();
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(false);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(security()));
        when(passwordEncoder.matches("secret", "HASH")).thenReturn(true);

        LoginResponse response = service.login(loginRequest(), http());

        assertEquals("A", response.getAccessToken());
        assertEquals("R", response.getRefreshToken());
        assertEquals(USERNAME, response.getUsername());
        verify(loginAttemptGuard).onSuccess(USERNAME);
        ArgumentCaptor<UserLoginLog> captor = ArgumentCaptor.forClass(UserLoginLog.class);
        verify(userLoginLogRepository).save(captor.capture());
        assertEquals((short) 1, captor.getValue().getSuccess());
        assertEquals((short) 0, captor.getValue().getLoginType());
        assertEquals(IP, captor.getValue().getIpAddress());
    }

    // ---------------- NFC 登录 ----------------

    @Test
    @DisplayName("NFC 登录：卡不存在抛 AUTH_REQUEST_NFC_NOT_FOUND 并记日志")
    void nfcLoginUnknownBadge() {
        UserNfcLoginDTO request = new UserNfcLoginDTO();
        request.setNfcId("NFC-9");
        when(nfcBadgeRepository.findByNfcUid("NFC-9")).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.loginNfc(request, http()));

        assertEquals(ErrorCode.AUTH_REQUEST_NFC_NOT_FOUND, ex.getErrorCode());
        ArgumentCaptor<UserLoginLog> captor = ArgumentCaptor.forClass(UserLoginLog.class);
        verify(userLoginLogRepository).save(captor.capture());
        assertEquals((short) 3, captor.getValue().getLoginType());
    }

    @Test
    @DisplayName("NFC 登录：用户被停用抛 AUTH_ACCOUNT_DISABLED")
    void nfcLoginDisabledUser() {
        UserNfcLoginDTO request = new UserNfcLoginDTO();
        request.setNfcId("NFC-1");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge(null)));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 0)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.loginNfc(request, http()));

        assertEquals(ErrorCode.AUTH_ACCOUNT_DISABLED, ex.getErrorCode());
    }

    @Test
    @DisplayName("NFC 登录：资料缺失抛 NOT_FOUND")
    void nfcLoginWithoutProfile() {
        UserNfcLoginDTO request = new UserNfcLoginDTO();
        request.setNfcId("NFC-1");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge(null)));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 1)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.loginNfc(request, http()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("NFC 登录：成功返回用户名/昵称/头像路径与待验证ID")
    void nfcLoginSuccessWithAvatar() {
        UserNfcLoginDTO request = new UserNfcLoginDTO();
        request.setNfcId("NFC-1");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge(null)));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 1)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(99L)));

        NfcLoginResponse response = service.loginNfc(request, http());

        assertEquals(USERNAME, response.getUsername());
        assertEquals("张三", response.getNickname());
        assertEquals("/api/profile/42/avatar/image", response.getAvatarFileUrl());
        assertNotNull(response.getVerificationId());
    }

    @Test
    @DisplayName("NFC 登录：无头像时头像地址为 null")
    void nfcLoginSuccessWithoutAvatar() {
        UserNfcLoginDTO request = new UserNfcLoginDTO();
        request.setNfcId("NFC-1");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge(null)));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 1)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(null)));

        assertNull(service.loginNfc(request, http()).getAvatarFileUrl());
    }

    // ---------------- NFC + PIN ----------------

    @Test
    @DisplayName("NFC+PIN：PIN 为空抛 VAL_PARAM_AUTH_PIN_EMPTY")
    void nfcPinRejectsBlankPin() {
        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId("NFC-1");
        request.setPin(" ");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.nfcPinLogin(request, http()));

        assertEquals(ErrorCode.VAL_PARAM_AUTH_PIN_EMPTY, ex.getErrorCode());
    }

    @Test
    @DisplayName("NFC+PIN：PIN 格式非 4-6 位数字被拒（免验证码端点的第一道闸）")
    void nfcPinRejectsBadFormat() {
        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId("NFC-1");
        request.setPin("12ab");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.nfcPinLogin(request, http()));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verifyNoInteractions(nfcBadgeRepository);
    }

    @Test
    @DisplayName("NFC+PIN：卡不存在抛 AUTH_REQUEST_NFC_NOT_FOUND，登录类型记为 3")
    void nfcPinUnknownBadge() {
        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId("NFC-9");
        request.setPin("1234");
        when(nfcBadgeRepository.findByNfcUid("NFC-9")).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.nfcPinLogin(request, http()));

        assertEquals(ErrorCode.AUTH_REQUEST_NFC_NOT_FOUND, ex.getErrorCode());
        ArgumentCaptor<UserLoginLog> captor = ArgumentCaptor.forClass(UserLoginLog.class);
        verify(userLoginLogRepository).save(captor.capture());
        assertEquals((short) 3, captor.getValue().getLoginType());
    }

    @Test
    @DisplayName("NFC+PIN：账号已锁定时直接拒绝")
    void nfcPinRejectsLockedAccount() {
        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId("NFC-1");
        request.setPin("1234");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge("PIN")));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 1)));
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.nfcPinLogin(request, http()));

        assertEquals(ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("NFC+PIN：IP 被封禁时直接拒绝")
    void nfcPinRejectsBlockedIp() {
        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId("NFC-1");
        request.setPin("1234");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge("PIN")));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 1)));
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.nfcPinLogin(request, http()));

        assertEquals(ErrorCode.AUTH_ACCOUNT_LOCKED, ex.getErrorCode());
    }

    @Test
    @DisplayName("NFC+PIN：卡上未设置 PIN 一律判失败")
    void nfcPinRejectsWhenPinHashMissing() {
        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId("NFC-1");
        request.setPin("1234");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge(null)));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 1)));
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(false);
        when(loginAttemptGuard.onFailure(USERNAME, IP)).thenReturn(1L);
        when(loginAttemptGuard.getMaxFailures()).thenReturn(5);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.nfcPinLogin(request, http()));

        assertEquals(ErrorCode.AUTH_REQUEST_PIN_ERROR, ex.getErrorCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("NFC+PIN：PIN 错误抛 AUTH_REQUEST_PIN_ERROR 并累计失败")
    void nfcPinRejectsWrongPin() {
        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId("NFC-1");
        request.setPin("1234");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge("PIN")));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 1)));
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(false);
        when(passwordEncoder.matches("1234", "PIN")).thenReturn(false);
        when(loginAttemptGuard.onFailure(USERNAME, IP)).thenReturn(2L);
        when(loginAttemptGuard.getMaxFailures()).thenReturn(5);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.nfcPinLogin(request, http()));

        assertEquals(ErrorCode.AUTH_REQUEST_PIN_ERROR, ex.getErrorCode());
        verify(loginAttemptGuard).onFailure(USERNAME, IP);
    }

    @Test
    @DisplayName("NFC+PIN：成功下发令牌")
    void nfcPinSuccess() {
        stubTokenSuccess();
        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId("NFC-1");
        request.setPin("1234");
        when(nfcBadgeRepository.findByNfcUid("NFC-1")).thenReturn(Optional.of(badge("PIN")));
        when(userCoreRepository.findById(any())).thenReturn(Optional.of(user((short) 1)));
        when(loginAttemptGuard.isLocked(USERNAME)).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(IP)).thenReturn(false);
        when(passwordEncoder.matches("1234", "PIN")).thenReturn(true);

        assertEquals("A", service.nfcPinLogin(request, http()).getAccessToken());
        verify(loginAttemptGuard).onSuccess(USERNAME);
    }

    // ---------------- 刷新 / 校验 ----------------

    @Test
    @DisplayName("刷新令牌：令牌无效抛 AUTH_INVALID_TOKEN")
    void refreshRejectsInvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad");
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.refreshToken(request, http()));

        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("刷新令牌：用访问令牌冒充刷新令牌被拒（类型不符）")
    void refreshRejectsWrongTokenType() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("access-token");
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.isTokenType("access-token", "refresh")).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.refreshToken(request, http()));

        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, ex.getErrorCode());
        verify(userCoreRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("刷新令牌：用户不存在抛 NOT_FOUND")
    void refreshUnknownUser() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("R");
        when(jwtTokenProvider.validateToken("R")).thenReturn(true);
        when(jwtTokenProvider.isTokenType("R", "refresh")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("R")).thenReturn(USERNAME);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.refreshToken(request, http()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("刷新令牌：成功换发新令牌")
    void refreshSuccess() {
        stubTokenSuccess();
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("R");
        when(jwtTokenProvider.validateToken("R")).thenReturn(true);
        when(jwtTokenProvider.isTokenType("R", "refresh")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("R")).thenReturn(USERNAME);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));

        assertEquals("A", service.refreshToken(request, http()).getAccessToken());
    }

    @Test
    @DisplayName("校验令牌：无效抛 AUTH_INVALID_TOKEN")
    void validateRejectsInvalidToken() {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.validateToken("bad"));

        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("校验令牌：刷新令牌不得当访问令牌用（类型不符）")
    void validateRejectsWrongTokenType() {
        when(jwtTokenProvider.validateToken("R")).thenReturn(true);
        when(jwtTokenProvider.isTokenType("R", "access")).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.validateToken("R"));

        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("校验令牌：用户被停用或状态为空抛 AUTH_ACCOUNT_DISABLED")
    void validateRejectsDisabledUser() {
        when(jwtTokenProvider.validateToken("A")).thenReturn(true);
        when(jwtTokenProvider.isTokenType("A", "access")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("A")).thenReturn(USERNAME);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user(null)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.validateToken("A"));

        assertEquals(ErrorCode.AUTH_ACCOUNT_DISABLED, ex.getErrorCode());
    }

    @Test
    @DisplayName("校验令牌：通过时返回用户名")
    void validateReturnsUsername() {
        when(jwtTokenProvider.validateToken("A")).thenReturn(true);
        when(jwtTokenProvider.isTokenType("A", "access")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("A")).thenReturn(USERNAME);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));

        assertEquals(USERNAME, service.validateToken("A"));
    }

    // ---------------- 权限 ----------------

    @Test
    @DisplayName("权限检查：用户不存在抛 NOT_FOUND")
    void checkPermissionUnknownUser() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.checkPermission(USERNAME, "p1"));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("权限检查：用户无任何角色抛 FORBIDDEN")
    void checkPermissionWithoutRoles() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of());

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.checkPermission(USERNAME, "p1"));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("权限检查：管理员（管理员/超级管理员/ADMIN）直接放行，不查权限表")
    void checkPermissionAdminBypasses() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));
        UserRole userRole = new UserRole();
        userRole.setRoleId(1L);
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(userRole));
        Role role = new Role();
        role.setName("超级管理员");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        service.checkPermission(USERNAME, "任意权限");

        verify(rolePermissionRepository, never()).findByRoleIdIn(any());
    }

    @Test
    @DisplayName("权限检查：非管理员但权限码命中则放行")
    void checkPermissionGrantedByRole() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));
        UserRole userRole = new UserRole();
        userRole.setRoleId(5L);
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(userRole));
        Role role = new Role();
        role.setName("操作员");
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(5L);
        rolePermission.setPermissionId(7L);
        when(rolePermissionRepository.findByRoleIdIn(List.of(5L)))
                .thenReturn(List.of(rolePermission));
        Permission permission = new Permission();
        permission.setCode("inventory:read");
        when(permissionRepository.findAllById(List.of(7L))).thenReturn(List.of(permission));

        service.checkPermission(USERNAME, "inventory:read");

        verify(permissionRepository).findAllById(List.of(7L));
    }

    @Test
    @DisplayName("权限检查：权限码不命中抛 FORBIDDEN")
    void checkPermissionDenied() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user((short) 1)));
        UserRole userRole = new UserRole();
        userRole.setRoleId(5L);
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(userRole));
        Role role = new Role();
        role.setName("操作员");
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(5L);
        rolePermission.setPermissionId(7L);
        when(rolePermissionRepository.findByRoleIdIn(List.of(5L)))
                .thenReturn(List.of(rolePermission));
        Permission permission = new Permission();
        permission.setCode("inventory:read");
        when(permissionRepository.findAllById(List.of(7L))).thenReturn(List.of(permission));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.checkPermission(USERNAME, "admin:all"));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    // ---------------- 登出 ----------------

    @Test
    @DisplayName("登出：令牌为空时不做任何事")
    void logoutWithNullTokenIsNoop() {
        service.logout(null);

        verifyNoInteractions(stringRedisTemplate);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("登出：清除该用户的访问与刷新令牌键")
    void logoutClearsBothTokenKeys() {
        when(jwtTokenProvider.getUsernameFromToken("A")).thenReturn(USERNAME);

        service.logout("A");

        verify(stringRedisTemplate).delete("auth:token:access:" + USERNAME);
        verify(stringRedisTemplate).delete("auth:token:refresh:" + USERNAME);
    }

    @Test
    @DisplayName("登出：把访问令牌（以及当时在册的刷新令牌）写进吊销集，而不是只删键")
    void logoutRevokesTokensRatherThanOnlyForgettingThem() {
        when(jwtTokenProvider.getUsernameFromToken("A")).thenReturn(USERNAME);
        when(valueOperations.get("auth:token:refresh:" + USERNAME)).thenReturn("R");
        // 两个令牌都要有剩余有效期，避免 TTL 走到兜底分支
        when(jwtTokenProvider.getExpirationDateFromToken(anyString()))
                .thenReturn(new java.util.Date(System.currentTimeMillis() + 3600_000L));

        service.logout("A");

        // 只删键的写法等于"服务端仍然认这张令牌"（validateToken 原本不读 Redis）
        verify(valueOperations, times(2))
                .set(
                        argThat(key -> key.startsWith("auth:revoked:token:")),
                        eq("1"),
                        any(java.time.Duration.class));
    }

    @Test
    @DisplayName("已登出的访问令牌不可再用（登出必须真的生效）")
    void validateTokenRejectsRevokedToken() {
        when(jwtTokenProvider.validateToken("A")).thenReturn(true);
        when(jwtTokenProvider.isTokenType("A", "access")).thenReturn(true);
        // 直接把"吊销集里有它"造出来：登出写进去的键就是 auth:revoked:token:<指纹>
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        BusinessException error =
                assertThrows(BusinessException.class, () -> service.validateToken("A"));

        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, error.getErrorCode());
        assertTrue(
                String.valueOf(error.getMessage()).contains("登出"),
                "错误信息要能看出是已登出而不是令牌损坏：" + error.getMessage());
    }
}

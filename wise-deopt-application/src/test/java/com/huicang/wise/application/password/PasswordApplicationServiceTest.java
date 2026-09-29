package com.huicang.wise.application.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.domain.user.UserProfile;
import com.huicang.wise.domain.user.UserSecurity;
import com.huicang.wise.infrastructure.persistence.repository.user.UserCoreRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserProfileRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserSecurityRepository;
import com.huicang.wise.infrastructure.security.PasswordEncoder;
import com.huicang.wise.infrastructure.security.PasswordPolicyValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 密码应用服务的单元测试：修改密码、管理员重置、忘记密码三条路径，以及密码落库时的 salt 兜底。
 *
 * <p>本批钉住一处**功能缺陷**（只记录、未修）：{@code forgotPassword} 生成了临时密码并**直接把库里的口令改掉**， 但方法签名是 {@code
 * void}、返回体与日志里都**没有**这个临时密码 —— 也就是说用户**无法得知**新口令， 该接口一旦被调用就等于把这个账号锁死。三条路径共同的 {@code
 * updatePassword} 还会在 salt 为空时补一个 32 位随机 salt。
 */
@ExtendWith(MockitoExtension.class)
class PasswordApplicationServiceTest {

    private static final long USER_ID = 42L;
    private static final String OLD_HASH = "OLD-HASH";

    @Mock private UserCoreRepository userCoreRepository;
    @Mock private UserSecurityRepository userSecurityRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordPolicyValidator passwordPolicyValidator;
    @Mock private PasswordPolicyValidator.ValidationResult validResult;
    @Mock private PasswordPolicyValidator.ValidationResult invalidResult;

    private PasswordApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new PasswordApplicationService(
                        userCoreRepository,
                        userSecurityRepository,
                        userProfileRepository,
                        passwordEncoder,
                        passwordPolicyValidator);
    }

    private UserCore user() {
        UserCore entity = new UserCore();
        entity.setUserId(USER_ID);
        entity.setUsername("zhang");
        return entity;
    }

    private UserSecurity security(String salt) {
        UserSecurity entity = new UserSecurity();
        entity.setPasswordHash(OLD_HASH);
        entity.setSalt(salt);
        return entity;
    }

    private ChangePasswordRequest changeRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-pass");
        request.setNewPassword("NewPass!2345");
        return request;
    }

    private void stubChangeHappyPath(UserSecurity userSecurity) {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userSecurity));
        when(passwordEncoder.matches("old-pass", OLD_HASH)).thenReturn(true);
        when(passwordPolicyValidator.validatePassword("NewPass!2345")).thenReturn(validResult);
        when(validResult.isValid()).thenReturn(true);
        when(passwordEncoder.encode("NewPass!2345")).thenReturn("NEW-HASH");
    }

    // ---------------- 修改密码 ----------------

    @Test
    @DisplayName("改密码：用户不存在抛 NOT_FOUND")
    void changePasswordMissingUser() {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.changePassword(USER_ID, changeRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("改密码：安全信息不存在抛 NOT_FOUND")
    void changePasswordMissingSecurity() {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.changePassword(USER_ID, changeRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("改密码：当前密码错误抛 AUTH_REQUEST_PASSWORD_VERIFY_FAILED，且不校验新密码、不落库")
    void changePasswordWrongCurrent() {
        UserSecurity userSecurity = security("salt-32");
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userSecurity));
        when(passwordEncoder.matches("old-pass", OLD_HASH)).thenReturn(false);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.changePassword(USER_ID, changeRequest()));

        assertEquals(ErrorCode.AUTH_REQUEST_PASSWORD_VERIFY_FAILED, ex.getErrorCode());
        verify(passwordPolicyValidator, never()).validatePassword(anyString());
        verify(userSecurityRepository, never()).save(any(UserSecurity.class));
    }

    @Test
    @DisplayName("改密码：新密码不符合策略抛 PARAM_ERROR，并带出策略给出的原因")
    void changePasswordPolicyViolation() {
        UserSecurity userSecurity = security("salt-32");
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userSecurity));
        when(passwordEncoder.matches("old-pass", OLD_HASH)).thenReturn(true);
        when(passwordPolicyValidator.validatePassword("NewPass!2345")).thenReturn(invalidResult);
        when(invalidResult.isValid()).thenReturn(false);
        when(invalidResult.getErrors()).thenReturn(List.of("长度不足", "缺少数字"));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.changePassword(USER_ID, changeRequest()));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("长度不足"), ex.getMessage());
        assertTrue(ex.getMessage().contains("缺少数字"), ex.getMessage());
        verify(userSecurityRepository, never()).save(any(UserSecurity.class));
    }

    @Test
    @DisplayName("改密码：成功后写入新哈希与改动时间，并保存")
    void changePasswordSuccess() {
        UserSecurity userSecurity = security("salt-32");
        stubChangeHappyPath(userSecurity);

        service.changePassword(USER_ID, changeRequest());

        assertEquals("NEW-HASH", userSecurity.getPasswordHash());
        assertNotNull(userSecurity.getLastPasswordChangeAt());
        verify(userSecurityRepository).save(userSecurity);
    }

    @Test
    @DisplayName("salt 兜底：为空时补 32 位随机十六进制")
    void fillsSaltWhenMissing() {
        UserSecurity userSecurity = security(null);
        stubChangeHappyPath(userSecurity);

        service.changePassword(USER_ID, changeRequest());

        assertNotNull(userSecurity.getSalt());
        assertEquals(32, userSecurity.getSalt().length());
        assertTrue(userSecurity.getSalt().matches("^[0-9a-f]{32}$"), userSecurity.getSalt());
    }

    @Test
    @DisplayName("salt 兜底：已有 salt 时保持原值")
    void keepsExistingSalt() {
        UserSecurity userSecurity = security("KEEP-ME");
        stubChangeHappyPath(userSecurity);

        service.changePassword(USER_ID, changeRequest());

        assertEquals("KEEP-ME", userSecurity.getSalt());
    }

    // ---------------- 管理员重置 ----------------

    @Test
    @DisplayName("重置密码：用户不存在抛 NOT_FOUND")
    void resetMissingUser() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUserId(USER_ID);
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.resetPassword(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("重置密码：安全信息不存在抛 NOT_FOUND")
    void resetMissingSecurity() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUserId(USER_ID);
        request.setNewPassword("NewPass!2345");
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.resetPassword(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("重置密码：不符合策略抛 PARAM_ERROR，且不落库")
    void resetPolicyViolation() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUserId(USER_ID);
        request.setNewPassword("weak");
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(security("s")));
        when(passwordPolicyValidator.validatePassword("weak")).thenReturn(invalidResult);
        when(invalidResult.isValid()).thenReturn(false);
        when(invalidResult.getErrors()).thenReturn(List.of("太弱"));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.resetPassword(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(userSecurityRepository, never()).save(any(UserSecurity.class));
    }

    @Test
    @DisplayName("重置密码：成功写入新哈希（不需要旧的当前密码）")
    void resetSuccess() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUserId(USER_ID);
        request.setNewPassword("NewPass!2345");
        UserSecurity userSecurity = security("salt-32");
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userSecurity));
        when(passwordPolicyValidator.validatePassword("NewPass!2345")).thenReturn(validResult);
        when(validResult.isValid()).thenReturn(true);
        when(passwordEncoder.encode("NewPass!2345")).thenReturn("RESET-HASH");

        service.resetPassword(request);

        assertEquals("RESET-HASH", userSecurity.getPasswordHash());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userSecurityRepository).save(userSecurity);
    }

    // ---------------- 忘记密码 ----------------

    @Test
    @DisplayName("忘记密码：邮箱未注册抛 NOT_FOUND")
    void forgotUnknownEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nobody@example.com");
        when(userProfileRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.forgotPassword(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("忘记密码：资料指向的用户不存在抛 NOT_FOUND")
    void forgotProfileWithoutUser() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("z@example.com");
        UserProfile profile = new UserProfile();
        profile.setUserId(USER_ID);
        when(userProfileRepository.findByEmail("z@example.com")).thenReturn(Optional.of(profile));
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.forgotPassword(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("忘记密码：安全信息不存在抛 NOT_FOUND")
    void forgotMissingSecurity() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("z@example.com");
        UserProfile profile = new UserProfile();
        profile.setUserId(USER_ID);
        when(userProfileRepository.findByEmail("z@example.com")).thenReturn(Optional.of(profile));
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.forgotPassword(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("现状缺陷：忘记密码生成了 12 位临时密码并直接改库，但调用方拿不到它")
    void forgotPasswordGeneratesUnreachableTempPassword() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("z@example.com");
        UserProfile profile = new UserProfile();
        profile.setUserId(USER_ID);
        UserSecurity userSecurity = security("salt-32");
        when(userProfileRepository.findByEmail("z@example.com")).thenReturn(Optional.of(profile));
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(userSecurityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userSecurity));

        ArgumentCaptor<String> tempCaptor = ArgumentCaptor.forClass(String.class);
        when(passwordEncoder.encode(tempCaptor.capture())).thenAnswer(invocation -> "TEMP-HASH");

        service.forgotPassword(request);

        String tempPassword = tempCaptor.getValue();
        assertEquals(12, tempPassword.length(), "现状：临时密码为 12 位");
        assertEquals("TEMP-HASH", userSecurity.getPasswordHash(), "现状：库里口令已被改成临时密码");
        assertNotNull(userSecurity.getLastPasswordChangeAt());
        verify(userSecurityRepository).save(userSecurity);
        // 现状：forgotPassword 的返回类型是 void，临时密码既未返回也未写入任何可见渠道
        assertTrue(
                void
                        .class.equals(
                                java.util.Arrays.stream(
                                                PasswordApplicationService.class.getMethods())
                                        .filter(m -> m.getName().equals("forgotPassword"))
                                        .findFirst()
                                        .orElseThrow()
                                        .getReturnType()),
                "现状：方法签名是 void，调用方无法得知临时密码");
    }

    @Test
    @DisplayName("密码强度：直接转发给策略校验器")
    void passwordStrengthDelegates() {
        when(passwordPolicyValidator.getPasswordStrength("abc")).thenReturn(2);

        assertEquals(2, service.getPasswordStrength("abc"));
    }

    @Test
    @DisplayName("salt 兜底：空白串同样会补一个 32 位 salt")
    void fillsSaltWhenBlank() {
        UserSecurity userSecurity = security("   ");
        stubChangeHappyPath(userSecurity);

        service.changePassword(USER_ID, changeRequest());

        assertEquals(32, userSecurity.getSalt().length());
        assertTrue(
                LocalDateTime.now()
                        .isAfter(userSecurity.getLastPasswordChangeAt().minusMinutes(1)));
    }
}

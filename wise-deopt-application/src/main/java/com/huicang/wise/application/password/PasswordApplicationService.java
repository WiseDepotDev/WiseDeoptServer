package com.huicang.wise.application.password;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.domain.user.UserSecurity;
import com.huicang.wise.domain.user.UserProfile;
import com.huicang.wise.domain.repository.user.UserCoreRepository;
import com.huicang.wise.domain.repository.user.UserSecurityRepository;
import com.huicang.wise.domain.repository.user.UserProfileRepository;
import com.huicang.wise.infrastructure.security.PasswordEncoder;
import com.huicang.wise.infrastructure.security.PasswordPolicyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class PasswordApplicationService {

    private final UserCoreRepository userCoreRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    private static final int PASSWORD_EXPIRY_DAYS = 90;

    public PasswordApplicationService(
            UserCoreRepository userCoreRepository,
            UserSecurityRepository userSecurityRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator) {
        this.userCoreRepository = userCoreRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserCore user = userCoreRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户安全信息不存在"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), userSecurity.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_REQUEST_PASSWORD_VERIFY_FAILED, "当前密码错误");
        }

        validateNewPassword(user, request.getNewPassword());

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        updatePassword(user, userSecurity, newPasswordHash);

        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserCore user = userCoreRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserSecurity userSecurity = userSecurityRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户安全信息不存在"));

        PasswordPolicyValidator.ValidationResult validationResult = passwordPolicyValidator.validatePassword(request.getNewPassword());
        if (!validationResult.isValid()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不符合要求: " + String.join(", ", validationResult.getErrors()));
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        updatePassword(user, userSecurity, newPasswordHash);

        log.info("管理员重置用户 {} 的密码成功", user.getUsername());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        UserProfile userProfile = userProfileRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "邮箱未注册"));

        UserCore user = userCoreRepository.findById(userProfile.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserSecurity userSecurity = userSecurityRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户安全信息不存在"));

        String tempPassword = generateTempPassword();
        String tempPasswordHash = passwordEncoder.encode(tempPassword);

        updatePassword(user, userSecurity, tempPasswordHash);

        log.info("用户 {} 请求重置密码，临时密码已生成", user.getUsername());
    }

    public int getPasswordStrength(String password) {
        return passwordPolicyValidator.getPasswordStrength(password);
    }

    private void validateNewPassword(UserCore user, String newPassword) {
        PasswordPolicyValidator.ValidationResult validationResult = passwordPolicyValidator.validatePassword(newPassword);
        if (!validationResult.isValid()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不符合要求: " + String.join(", ", validationResult.getErrors()));
        }
    }

    private void updatePassword(UserCore user, UserSecurity userSecurity, String newPasswordHash) {
        userSecurity.setPasswordHash(newPasswordHash);
        userSecurity.setLastPasswordChangeAt(LocalDateTime.now());
        
        // 确保salt字段不为空，以满足数据库约束
        if (userSecurity.getSalt() == null || userSecurity.getSalt().isBlank()) {
            // 生成一个随机的32位字符串作为salt
            String salt = UUID.randomUUID().toString().replace("-", "");
            if (salt.length() > 32) {
                salt = salt.substring(0, 32);
            }
            userSecurity.setSalt(salt);
        }
        
        userSecurityRepository.save(userSecurity);
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }
}

package com.huicang.wise.application.auth;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.user.UserCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.user.UserCoreRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.Duration;
import java.util.UUID;

/**
 * 类功能描述：认证应用服务
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 实现基础登录逻辑与令牌缓存
 */
@Service
public class AuthApplicationService {

    private final UserCoreRepository userCoreRepository;

    private final StringRedisTemplate stringRedisTemplate;

    public AuthApplicationService(UserCoreRepository userCoreRepository,
                                  StringRedisTemplate stringRedisTemplate) {
        this.userCoreRepository = userCoreRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 方法功能描述：执行用户登录并生成令牌
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     * @throws BusinessException 当用户不存在或密码错误时抛出业务异常
     */
    public LoginResponse login(LoginRequest request) throws BusinessException {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_USERNAME_EMPTY, ErrorCode.AUTH_USERNAME_EMPTY.getMessage());
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_EMPTY, ErrorCode.AUTH_PASSWORD_EMPTY.getMessage());
        }
        UserCoreJpaEntity user = userCoreRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_LOGIN_FAILED, ErrorCode.AUTH_LOGIN_FAILED.getMessage()));
        
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED, ErrorCode.AUTH_ACCOUNT_DISABLED.getMessage());
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage());
        }

        // 校验密码
        String inputHash = hash(request.getPassword());
        if (user.getPasswordHash() != null && !user.getPasswordHash().equals(inputHash)) {
            int failCount = user.getLoginFailCount() == null ? 0 : user.getLoginFailCount();
            failCount++;
            user.setLoginFailCount(failCount);
            if (failCount >= 5) {
                user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(30));
                userCoreRepository.save(user);
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, "密码错误次数过多，" + ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage());
            }
            userCoreRepository.save(user);
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED, ErrorCode.AUTH_LOGIN_FAILED.getMessage());
        }

        // 登录成功，重置失败次数
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        userCoreRepository.save(user);

        return generateTokens(user);
    }

    /**
     * 方法功能描述：NFC扫码识别用户
     *
     * @param request NFC登录请求
     * @return NFC登录响应
     */
    public NfcLoginResponse loginNfc(UserNfcLoginDTO request) {
        UserCoreJpaEntity user = userCoreRepository.findByNfcId(request.getNfcId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_NFC_NOT_FOUND, ErrorCode.AUTH_NFC_NOT_FOUND.getMessage()));

        NfcLoginResponse response = new NfcLoginResponse();
        response.setUsername(user.getUsername());
        // 生成临时验证ID，后续可用于关联PIN验证（此处简化处理，仅返回随机ID）
        response.setVerificationId(UUID.randomUUID().toString());
        return response;
    }

    /**
     * 方法功能描述：NFC+PIN登录
     *
     * @param request NFC+PIN登录请求
     * @return 登录响应
     */
    public LoginResponse nfcPinLogin(NfcPinDTO request) {
        UserCoreJpaEntity user = userCoreRepository.findByNfcId(request.getNfcId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_NFC_NOT_FOUND, ErrorCode.AUTH_NFC_NOT_FOUND.getMessage()));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED, ErrorCode.AUTH_ACCOUNT_DISABLED.getMessage());
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage());
        }

        // 校验PIN码
        String inputHash = hash(request.getPin());
        if (user.getPinHash() != null && !user.getPinHash().equals(inputHash)) {
            int failCount = user.getLoginFailCount() == null ? 0 : user.getLoginFailCount();
            failCount++;
            user.setLoginFailCount(failCount);
            if (failCount >= 5) {
                user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(30));
                userCoreRepository.save(user);
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, "PIN码错误次数过多，" + ErrorCode.AUTH_ACCOUNT_LOCKED.getMessage());
            }
            userCoreRepository.save(user);
            throw new BusinessException(ErrorCode.AUTH_PIN_ERROR, ErrorCode.AUTH_PIN_ERROR.getMessage());
        }

        // 登录成功，重置失败次数
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        userCoreRepository.save(user);

        return generateTokens(user);
    }

    private String hash(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private LoginResponse generateTokens(UserCoreJpaEntity user) {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        String accessKey = "auth:token:access:" + accessToken;
        String refreshKey = "auth:token:refresh:" + refreshToken;
        stringRedisTemplate.opsForValue().set(accessKey, user.getUsername(), Duration.ofHours(2));
        stringRedisTemplate.opsForValue().set(refreshKey, user.getUsername(), Duration.ofDays(7));
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUsername(user.getUsername());
        return response;
    }
}

package com.huicang.wise.application.auth;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.auth.*;
import com.huicang.wise.infrastructure.repository.user.UserCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.user.UserCoreRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthApplicationService(UserCoreRepository userCoreRepository,
                                  RoleRepository roleRepository,
                                  RolePermissionRepository rolePermissionRepository,
                                  PermissionRepository permissionRepository,
                                  StringRedisTemplate stringRedisTemplate) {
        this.userCoreRepository = userCoreRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
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
        response.setNickname(user.getNickname());
        response.setAvatarFileUrl(user.getAvatar());
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

    /**
     * 方法功能描述：验证令牌并返回用户名
     *
     * @param token 访问令牌
     * @return 用户名
     */
    public String validateToken(String token) {
        String accessKey = "auth:token:access:" + token;
        String username = stringRedisTemplate.opsForValue().get(accessKey);
        if (username == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "无效的访问令牌");
        }
        return username;
    }

    /**
     * 方法功能描述：检查用户权限
     *
     * @param username 用户名
     * @param permissionCode 权限编码
     */
    public void checkPermission(String username, String permissionCode) {
        UserCoreJpaEntity user = userCoreRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        String roleCode = user.getRole();
        if (roleCode == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户无角色");
        }

        // 超级管理员拥有所有权限
        if ("admin".equals(roleCode)) {
            return;
        }

        RoleJpaEntity role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "角色未定义"));

        List<RolePermissionJpaEntity> rolePermissions = rolePermissionRepository.findByRoleId(role.getRoleId());
        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermissionJpaEntity::getPermissionId)
                .collect(Collectors.toList());

        List<PermissionJpaEntity> permissions = permissionRepository.findAllById(permissionIds);
        boolean hasPermission = permissions.stream()
                .anyMatch(p -> p.getPermissionCode().equals(permissionCode));

        if (!hasPermission) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此资源");
        }
    }

    /**
     * 方法功能描述：退出登录
     *
     * @param token 访问令牌
     */
    public void logout(String token) {
        String accessKey = "auth:token:access:" + token;
        // 同时删除关联的Refresh Token（如果需要更严格的安全控制）
        // 这里简化处理，只删除Access Token
        stringRedisTemplate.delete(accessKey);
    }
}

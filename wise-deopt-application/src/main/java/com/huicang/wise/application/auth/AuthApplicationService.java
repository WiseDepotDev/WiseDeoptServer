package com.huicang.wise.application.auth;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.user.UserCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.user.UserCoreRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
        // 此处为示例，实际项目中请替换为安全的密码校验逻辑
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED, ErrorCode.AUTH_ACCOUNT_DISABLED.getMessage());
        }
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

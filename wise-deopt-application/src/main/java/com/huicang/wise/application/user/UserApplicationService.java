package com.huicang.wise.application.user;

import com.huicang.wise.application.captcha.CaptchaApplicationService;
import com.huicang.wise.application.common.DeleteWithCaptchaRequest;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.domain.user.UserProfile;
import com.huicang.wise.domain.user.UserSecurity;
import com.huicang.wise.domain.repository.user.UserCoreRepository;
import com.huicang.wise.domain.repository.user.UserProfileRepository;
import com.huicang.wise.domain.repository.user.UserSecurityRepository;
import com.huicang.wise.application.oss.FileStorageApplicationService;
import com.huicang.wise.application.password.ChangePasswordRequest;
import com.huicang.wise.application.password.PasswordApplicationService;
import com.huicang.wise.application.role.RoleDTO;
import com.huicang.wise.application.user.UserRoleApplicationService;
import com.huicang.wise.domain.repository.auth.RoleRepository;
import com.huicang.wise.domain.repository.auth.UserRoleRepository;
import com.huicang.wise.domain.repository.auth.KeyAccessAuditLogRepository;
import com.huicang.wise.domain.repository.auth.UserLoginLogRepository;
import com.huicang.wise.domain.repository.user.NfcBadgeRepository;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.huicang.wise.application.user.UserRoleApplicationService;

/**
 * 类功能描述：用户管理应用服务
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Service
public class UserApplicationService {

    private final UserCoreRepository userCoreRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final PasswordApplicationService passwordApplicationService;
    private final UserRoleApplicationService userRoleApplicationService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final NfcBadgeRepository nfcBadgeRepository;
    private final KeyAccessAuditLogRepository keyAccessAuditLogRepository;
    private final UserLoginLogRepository userLoginLogRepository;
    private final FileStorageApplicationService fileStorageApplicationService;
    private final CaptchaApplicationService captchaApplicationService;

    public UserApplicationService(UserCoreRepository userCoreRepository,
                                UserProfileRepository userProfileRepository,
                                UserSecurityRepository userSecurityRepository,
                                PasswordApplicationService passwordApplicationService,
                                UserRoleApplicationService userRoleApplicationService,
                                RoleRepository roleRepository,
                                UserRoleRepository userRoleRepository,
                                NfcBadgeRepository nfcBadgeRepository,
                                KeyAccessAuditLogRepository keyAccessAuditLogRepository,
                                UserLoginLogRepository userLoginLogRepository,
                                FileStorageApplicationService fileStorageApplicationService,
                                CaptchaApplicationService captchaApplicationService) {
        this.userCoreRepository = userCoreRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.passwordApplicationService = passwordApplicationService;
        this.userRoleApplicationService = userRoleApplicationService;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.nfcBadgeRepository = nfcBadgeRepository;
        this.keyAccessAuditLogRepository = keyAccessAuditLogRepository;
        this.userLoginLogRepository = userLoginLogRepository;
        this.fileStorageApplicationService = fileStorageApplicationService;
        this.captchaApplicationService = captchaApplicationService;
    }

    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        if (userCoreRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }

        UserCore user = new UserCore();
        user.setUserId(System.nanoTime() + (long)(Math.random() * 1000));
        user.setUsername(request.getUsername());
        user.setUserType((short) 0);
        user.setStatus((short) 1);
        user.setIsDeleted((short) 0);
        user.setCreateBy(1L);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateBy(1L);
        user.setUpdateTime(LocalDateTime.now());
        UserCore savedUser = userCoreRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(savedUser.getUserId());
        profile.setNickname(request.getNickname());
        profile.setEmail(request.getEmail());
        profile.setGender(0);
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateBy(Long.valueOf(1));
        profile.setUpdateTime(LocalDateTime.now());
        userProfileRepository.save(profile);

        return toUserDTO(savedUser, profile);
    }

    @CacheEvict(prefix = "user", key = "#request.userId", allEntries = false)
    @Transactional
    public UserDTO updateUser(UserUpdateRequest request) {
        UserCore user = userCoreRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserProfile profile = userProfileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户资料不存在"));

        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        if (request.getNickname() != null) {
            profile.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            try {
                profile.setAvatarFileId(Long.parseLong(request.getAvatar()));
            } catch (NumberFormatException e) {
                profile.setAvatarFileId(null);
            }
        }
        if (request.getEnabled() != null) {
            user.setStatus(request.getEnabled() ? (short) 1 : (short) 0);
        }
        if (request.getRole() != null) {
            updateRole(user.getUserId(), request.getRole());
        }
        profile.setUpdateBy(Long.valueOf(1));
        profile.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(Long.valueOf(1));
        user.setUpdateTime(LocalDateTime.now());

        userCoreRepository.save(user);
        UserProfile savedProfile = userProfileRepository.save(profile);
        return toUserDTO(user, savedProfile);
    }

    @Cacheable(prefix = "user", key = "#userId", timeout = 1800)
    public UserDTO getUser(Long userId) {
        UserCore user = userCoreRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户资料不存在"));
        return toUserDTO(user, profile);
    }

    @Cacheable(prefix = "user:username", key = "#username", timeout = 1800)
    public UserDTO getUserByUsername(String username) {
        UserCore user = userCoreRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        UserProfile profile = userProfileRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户资料不存在"));
        return toUserDTO(user, profile);
    }

    public UserPageDTO listUsers(Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        Page<UserCore> userPage = userCoreRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")));

        UserPageDTO dto = new UserPageDTO();
        dto.setTotal(userPage.getTotalElements());
        List<UserDTO> items = userPage.getContent().stream()
                .map(user -> {
                    UserProfile profile = userProfileRepository.findByUserId(user.getUserId()).orElse(null);
                    return toUserDTO(user, profile);
                })
                .collect(Collectors.toList());
        dto.setItems(items);
        return dto;
    }

    @CacheEvict(prefix = "user", key = "#userId", allEntries = false)
    @Transactional
    public void deleteUser(Long userId) {
        if (!userCoreRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        
        // 删除关联数据
        userProfileRepository.deleteByUserId(userId);
        if (userSecurityRepository.existsByUserId(userId)) {
            userSecurityRepository.deleteByUserId(userId);
        }
        userRoleApplicationService.removeUserRoles(userId);
        
        // 删除NFC工牌
        nfcBadgeRepository.deleteByUserId(userId);
        // 删除访问密钥审计日志
        keyAccessAuditLogRepository.deleteByUserId(userId);
        // 删除用户登录日志
        userLoginLogRepository.deleteByUserId(userId);
        
        userCoreRepository.deleteById(userId);
    }

    @CacheEvict(prefix = "user", key = "#request.id", allEntries = false)
    @Transactional
    public void deleteUserWithCaptcha(DeleteWithCaptchaRequest request) {
        // 验证码为必填项：防止绕过验证码直接删除用户
        captchaApplicationService.enforceCaptcha(request.getCaptchaId(), request.getCaptchaCode());

        deleteUser(request.getId());
    }

    @Transactional
    public void changePassword(Long userId, UserPasswordChangeRequest request) {
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword(request.getOldPassword());
        changePasswordRequest.setNewPassword(request.getNewPassword());
        passwordApplicationService.changePassword(userId, changePasswordRequest);
    }

    private void updateRole(Long userId, String roleCode) {
        userRoleApplicationService.removeUserRoles(userId);
        
        String roleName = mapRoleCodeToName(roleCode);
        com.huicang.wise.domain.auth.Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在: " + roleCode));
        
        com.huicang.wise.domain.auth.UserRole userRole = new com.huicang.wise.domain.auth.UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getRoleId());
        userRole.setCreateBy(userId);
        userRole.setCreateTime(LocalDateTime.now());
        userRoleRepository.save(userRole);
    }

    private String mapRoleCodeToName(String roleCode) {
        if (roleCode == null) {
            return "访客";
        }
        switch (roleCode) {
            case "ADMIN":
                return "管理员";
            case "USER":
            default:
                return "访客";
        }
    }

    private UserDTO toUserDTO(UserCore user, UserProfile profile) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setNickname(profile != null ? profile.getNickname() : null);
        
        if (profile != null && profile.getAvatarFileId() != null) {
            // 返回头像API访问路径
            String avatarUrl = "/api/profile/" + user.getUserId() + "/avatar/image";
            dto.setAvatar(avatarUrl);
        } else {
            dto.setAvatar(null);
        }
        
        dto.setEmail(profile != null ? profile.getEmail() : null);
        dto.setEnabled(user.getStatus() == 1);
        dto.setNfcId(null);
        dto.setCreatedAt(user.getCreateTime());
        dto.setUpdatedAt(user.getUpdateTime());
        
        List<RoleDTO> roles = userRoleApplicationService.getUserRoles(user.getUserId());
        if (!roles.isEmpty()) {
            dto.setRole(roles.get(0).getRoleCode());
        }
        
        return dto;
    }
}

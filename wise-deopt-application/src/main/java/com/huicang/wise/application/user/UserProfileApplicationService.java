package com.huicang.wise.application.user;

import com.huicang.wise.application.oss.FileStorageApplicationService;
import com.huicang.wise.application.oss.FileUploadRequest;
import com.huicang.wise.application.oss.FileUploadResponse;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.domain.repository.user.UserCoreRepository;
import com.huicang.wise.domain.user.UserProfile;
import com.huicang.wise.domain.repository.user.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.huicang.wise.domain.repository.auth.RoleRepository;
import com.huicang.wise.domain.repository.auth.UserRoleRepository;
import com.huicang.wise.domain.auth.UserRole;
import com.huicang.wise.domain.auth.Role;
import java.util.List;

/**
 * 类功能描述：个人资料应用服务
 *
 * @author xingchentye
 * @version 0.1.24
 * @since 2026-02-27
 */
@Service
public class UserProfileApplicationService {

    private final UserCoreRepository userCoreRepository;
    private final UserProfileRepository userProfileRepository;
    private final FileStorageApplicationService fileStorageApplicationService;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserProfileApplicationService(UserCoreRepository userCoreRepository, 
                                         UserProfileRepository userProfileRepository,
                                         FileStorageApplicationService fileStorageApplicationService,
                                         UserRoleRepository userRoleRepository,
                                         RoleRepository roleRepository) {
        this.userCoreRepository = userCoreRepository;
        this.userProfileRepository = userProfileRepository;
        this.fileStorageApplicationService = fileStorageApplicationService;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * 方法功能描述：获取当前用户个人资料
     *
     * @param username 用户名
     * @return 用户个人资料
     */
    public UserProfileDTO getUserProfile(String username) {
        UserCore user = userCoreRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserProfile profile = userProfileRepository.findByUserId(user.getUserId())
                .orElse(null);

        return toUserProfileDTO(user, profile);
    }

    /**
     * 方法功能描述：更新当前用户个人资料
     *
     * @param username 用户名
     * @param request  更新请求
     * @return 更新后的个人资料
     */
    @Transactional
    public UserProfileDTO updateUserProfile(String username, UserProfileUpdateRequest request) {
        UserCore user = userCoreRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserProfile profile = userProfileRepository.findByUserId(user.getUserId())
                .orElse(new UserProfile());

        profile.setUserId(user.getUserId());
        profile.setCreateTime(profile.getCreateTime() != null ? profile.getCreateTime() : LocalDateTime.now());

        if (request.getNickname() != null) {
            profile.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getAvatarFileId() != null) {
            profile.setAvatarFileId(request.getAvatarFileId());
        }

        profile.setUpdateTime(LocalDateTime.now());
        profile.setUpdateBy(user.getUserId());

        UserProfile savedProfile = userProfileRepository.save(profile);

        return toUserProfileDTO(user, savedProfile);
    }

    /**
     * 方法功能描述：获取用户设置
     *
     * @param username 用户名
     * @return 用户设置
     */
    public UserSettingsDTO getUserSettings(String username) {
        UserCore user = userCoreRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserProfile profile = userProfileRepository.findByUserId(user.getUserId())
                .orElse(null);

        UserSettingsDTO dto = new UserSettingsDTO();
        dto.setUserId(user.getUserId());
        dto.setSettings(new HashMap<>());

        if (profile != null && profile.getNickname() != null) {
            dto.getSettings().put("nickname", profile.getNickname());
        }

        return dto;
    }

    /**
     * 方法功能描述：更新用户设置
     *
     * @param username 用户名
     * @param request  更新请求
     * @return 更新后的设置
     */
    @Transactional
    public UserSettingsDTO updateUserSettings(String username, UserSettingsUpdateRequest request) {
        UserCore user = userCoreRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserProfile profile = userProfileRepository.findByUserId(user.getUserId())
                .orElse(new UserProfile());

        Map<String, String> settings = request.getSettings();
        if (settings != null) {
            if (settings.containsKey("nickname")) {
                profile.setNickname(settings.get("nickname"));
            }
            profile.setUpdateTime(LocalDateTime.now());
            profile.setUpdateBy(user.getUserId());
            userProfileRepository.save(profile);
        }

        UserSettingsDTO dto = new UserSettingsDTO();
        dto.setUserId(user.getUserId());
        dto.setSettings(new HashMap<>());

        if (profile.getNickname() != null) {
            dto.getSettings().put("nickname", profile.getNickname());
        }
        return dto;
    }

    /**
     * 方法功能描述：删除用户头像
     *
     * @param username 用户名
     * @return 更新后的个人资料
     */
    @Transactional
    public UserProfileDTO deleteAvatar(String username) {
        UserCore user = userCoreRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        UserProfile profile = userProfileRepository.findByUserId(user.getUserId())
                .orElse(new UserProfile());

        if (profile.getAvatarFileId() != null) {
            // Optional: Delete the actual file from MinIO if needed, 
            // or just remove the reference. Here we just remove reference.
            // If we want to delete file:
            // try {
            //     fileStorageApplicationService.deleteFile(profile.getAvatarFileId());
            // } catch (Exception e) {
            //     // ignore
            // }
            
            profile.setAvatarFileId(null);
            profile.setUpdateTime(LocalDateTime.now());
            profile.setUpdateBy(user.getUserId());
            userProfileRepository.save(profile);
        }

        return toUserProfileDTO(user, profile);
    }

    private UserProfileDTO toUserProfileDTO(UserCore user, UserProfile profile) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());

        if (profile != null) {
            dto.setProfileId(profile.getProfileId());
            dto.setNickname(profile.getNickname());
            dto.setEmail(profile.getEmail());
            dto.setGender(profile.getGender());
            dto.setAvatarFileId(profile.getAvatarFileId());
            
            if (profile.getAvatarFileId() != null) {
                // 返回头像API访问路径
                String avatarUrl = "/api/profile/" + user.getUserId() + "/avatar/image";
                dto.setAvatarUrl(avatarUrl);
            }
        }

        return dto;
    }

    /**
     * 获取用户头像内容
     *
     * @param userId            目标用户ID
     * @param requesterUsername 请求者用户名
     * @return 头像文件内容
     */
    public byte[] getAvatarContent(Long userId, String requesterUsername) {
        UserCore requester = userCoreRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "请求用户不存在"));

        boolean isSelf = requester.getUserId().equals(userId);
        boolean isAdmin = false;

        if (!isSelf) {
            List<UserRole> roles = userRoleRepository.findByUserId(requester.getUserId());
            for (UserRole ur : roles) {
                Role r = roleRepository.findById(ur.getRoleId()).orElse(null);
                if (r != null && ("ADMIN".equalsIgnoreCase(r.getName()) || "超级管理员".equals(r.getName()) || "管理员".equals(r.getName()))) {
                    isAdmin = true;
                    break;
                }
            }
        }

        if (!isSelf && !isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该头像");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null || profile.getAvatarFileId() == null) {
            return null;
        }

        return fileStorageApplicationService.downloadFile(profile.getAvatarFileId());
    }

    /**
     * 方法功能描述：上传用户头像
     *
     * @param username 用户名
     * @param file     头像文件
     * @return 更新后的个人资料
     */
    @Transactional
    public UserProfileDTO uploadAvatar(String username, MultipartFile file) {
        UserCore user = userCoreRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        FileUploadRequest uploadRequest = new FileUploadRequest();
        uploadRequest.setFile(file);
        
        // 上传文件
        FileUploadResponse response = fileStorageApplicationService.uploadFile(uploadRequest, user.getUserId());

        UserProfile profile = userProfileRepository.findByUserId(user.getUserId())
                .orElse(new UserProfile());

        profile.setUserId(user.getUserId());
        if (profile.getCreateTime() == null) {
            profile.setCreateTime(LocalDateTime.now());
        }
        
        profile.setAvatarFileId(response.getFileId());
        profile.setUpdateTime(LocalDateTime.now());
        profile.setUpdateBy(user.getUserId());

        UserProfile savedProfile = userProfileRepository.save(profile);

        return toUserProfileDTO(user, savedProfile);
    }
}
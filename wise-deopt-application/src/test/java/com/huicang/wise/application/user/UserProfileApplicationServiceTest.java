package com.huicang.wise.application.user;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.oss.FileStorageApplicationService;
import com.huicang.wise.application.oss.FileUploadResponse;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.Role;
import com.huicang.wise.domain.auth.UserRole;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.domain.user.UserProfile;
import com.huicang.wise.infrastructure.persistence.repository.auth.RoleRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.UserRoleRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserCoreRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserProfileRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人资料应用服务的单元测试：资料读写、设置读写、头像上传与删除、以及**头像读取的授权判定**。
 *
 * <p>本批把两类内容钉住： ① **授权**：{@code getAvatarContent} 只允许"本人"或"管理员"（管理员按角色名判：ADMIN 大小写不敏感 / 超级管理员 /
 * 管理员）， 其余一律 {@code FORBIDDEN}；这是本类唯一的越权敏感点，逐条覆盖。 ② **现状缺陷（本批已修其一）**：{@code updateUserSettings}
 * 的新建资料分支原本漏了 {@code setUserId}， 会写出一条 userId 为空的资料行；已按 {@code updateUserProfile} 的口径补齐，并用断言固定。
 * 另一条**未修**：{@code deleteAvatar} 只解除引用、**不删除对象存储里的真实文件**（代码注释里也承认了），本批钉住现状。
 */
@ExtendWith(MockitoExtension.class)
class UserProfileApplicationServiceTest {

    private static final long USER_ID = 42L;
    private static final long AVATAR_FILE_ID = 99L;
    private static final String USERNAME = "zhang";

    @Mock private UserCoreRepository userCoreRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private FileStorageApplicationService fileStorageApplicationService;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private MultipartFile multipartFile;

    private UserProfileApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new UserProfileApplicationService(
                        userCoreRepository,
                        userProfileRepository,
                        fileStorageApplicationService,
                        userRoleRepository,
                        roleRepository);
    }

    private UserCore user() {
        UserCore entity = new UserCore();
        entity.setUserId(USER_ID);
        entity.setUsername(USERNAME);
        entity.setStatus((short) 1);
        entity.setCreateTime(LocalDateTime.now().minusDays(1));
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    private UserProfile profile(Long avatarFileId) {
        UserProfile entity = new UserProfile();
        entity.setUserId(USER_ID);
        entity.setProfileId(7L);
        entity.setNickname("张三");
        entity.setEmail("z@example.com");
        entity.setCreateTime(LocalDateTime.now().minusDays(1));
        entity.setAvatarFileId(avatarFileId);
        return entity;
    }

    private UserProfileUpdateRequest profileUpdate() {
        return new UserProfileUpdateRequest();
    }

    // ---------------- 资料读取 ----------------

    @Test
    @DisplayName("资料读取：用户不存在抛 NOT_FOUND")
    void getUserProfileMissingUser() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getUserProfile(USERNAME));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("资料读取：无资料行时返回基础字段、昵称为 null")
    void getUserProfileWithoutProfileRow() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        UserProfileDTO dto = service.getUserProfile(USERNAME);

        assertEquals(USER_ID, dto.getUserId().longValue());
        assertEquals(USERNAME, dto.getUsername());
        assertNull(dto.getNickname());
        assertNull(dto.getAvatarUrl());
    }

    @Test
    @DisplayName("资料读取：有头像时给出头像接口路径")
    void getUserProfileWithAvatar() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile(AVATAR_FILE_ID)));

        UserProfileDTO dto = service.getUserProfile(USERNAME);

        assertEquals("张三", dto.getNickname());
        assertEquals(AVATAR_FILE_ID, dto.getAvatarFileId().longValue());
        assertEquals("/api/profile/42/avatar/image", dto.getAvatarUrl());
        assertEquals(7L, dto.getProfileId().longValue());
    }

    // ---------------- 资料更新 ----------------

    @Test
    @DisplayName("资料更新：用户不存在抛 NOT_FOUND")
    void updateUserProfileMissingUser() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateUserProfile(USERNAME, profileUpdate()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("资料更新：无资料行时新建，并补上 userId 与 createTime")
    void updateUserProfileCreatesRow() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateUserProfile(USERNAME, profileUpdate());

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        UserProfile saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId().longValue());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        assertEquals(USER_ID, saved.getUpdateBy().longValue());
    }

    @Test
    @DisplayName("资料更新：已有资料行时保留原 createTime")
    void updateUserProfileKeepsCreateTime() {
        UserProfile existing = profile(null);
        LocalDateTime original = existing.getCreateTime();
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateUserProfile(USERNAME, profileUpdate());

        assertEquals(original, existing.getCreateTime());
    }

    @Test
    @DisplayName("资料更新：只覆盖请求里非空字段")
    void updateUserProfileAppliesOnlyNonNullFields() {
        UserProfile existing = profile(null);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UserProfileUpdateRequest request = profileUpdate();
        request.setEmail("new@example.com");

        service.updateUserProfile(USERNAME, request);

        assertEquals("new@example.com", existing.getEmail());
        assertEquals("张三", existing.getNickname());
    }

    @Test
    @DisplayName("资料更新：昵称/性别/头像ID 均被写入")
    void updateUserProfileAppliesAllFields() {
        UserProfile existing = profile(null);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UserProfileUpdateRequest request = profileUpdate();
        request.setNickname("李四");
        request.setGender(1);
        request.setAvatarFileId(AVATAR_FILE_ID);

        UserProfileDTO dto = service.updateUserProfile(USERNAME, request);

        assertEquals("李四", existing.getNickname());
        assertEquals(AVATAR_FILE_ID, existing.getAvatarFileId().longValue());
        assertEquals("/api/profile/42/avatar/image", dto.getAvatarUrl());
    }

    // ---------------- 设置 ----------------

    @Test
    @DisplayName("设置读取：用户不存在抛 NOT_FOUND")
    void getUserSettingsMissingUser() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getUserSettings(USERNAME));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("设置读取：无资料行时返回空设置")
    void getUserSettingsWithoutProfile() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        UserSettingsDTO dto = service.getUserSettings(USERNAME);

        assertEquals(USER_ID, dto.getUserId().longValue());
        assertEquals(0, dto.getSettings().size());
    }

    @Test
    @DisplayName("设置读取：有昵称时带出 nickname")
    void getUserSettingsWithNickname() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(null)));

        assertEquals("张三", service.getUserSettings(USERNAME).getSettings().get("nickname"));
    }

    @Test
    @DisplayName("设置更新：用户不存在抛 NOT_FOUND")
    void updateUserSettingsMissingUser() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.updateUserSettings(
                                        USERNAME, new UserSettingsUpdateRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("设置更新：settings 为 null 时不落库")
    void updateUserSettingsWithNullSettings() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(null)));

        UserSettingsDTO dto = service.updateUserSettings(USERNAME, new UserSettingsUpdateRequest());

        assertEquals("张三", dto.getSettings().get("nickname"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("设置更新：无 nickname 键时仍落库但不改昵称")
    void updateUserSettingsWithoutNicknameKey() {
        UserProfile existing = profile(null);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest();
        request.setSettings(Map.of("theme", "dark"));

        service.updateUserSettings(USERNAME, request);

        assertEquals("张三", existing.getNickname());
        verify(userProfileRepository).save(existing);
    }

    @Test
    @DisplayName("设置更新：有 nickname 键时写入昵称")
    void updateUserSettingsWithNicknameKey() {
        UserProfile existing = profile(null);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest();
        request.setSettings(Map.of("nickname", "王五"));

        UserSettingsDTO dto = service.updateUserSettings(USERNAME, request);

        assertEquals("王五", dto.getSettings().get("nickname"));
        assertEquals("王五", existing.getNickname());
    }

    @Test
    @DisplayName("设置更新：无资料行时新建的资料**必须挂上 userId**（本批修复）")
    void updateUserSettingsNewRowCarriesUserId() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest();
        request.setSettings(Map.of("nickname", "王五"));

        service.updateUserSettings(USERNAME, request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertEquals(
                USER_ID,
                captor.getValue().getUserId().longValue(),
                "修复：新建资料行必须挂上 userId，否则违反 user_profile.user_id 非空约束");
    }

    // ---------------- 头像：删除 / 授权 / 上传 ----------------

    @Test
    @DisplayName("删除头像：用户不存在抛 NOT_FOUND")
    void deleteAvatarMissingUser() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deleteAvatar(USERNAME));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("删除头像：有头像时解除引用并落库（不删对象存储里的文件，现状）")
    void deleteAvatarClearsReference() {
        UserProfile existing = profile(AVATAR_FILE_ID);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

        UserProfileDTO dto = service.deleteAvatar(USERNAME);

        assertNull(existing.getAvatarFileId());
        assertNull(dto.getAvatarUrl());
        verify(userProfileRepository).save(existing);
        // 现状：只解除引用，不去删 MinIO 里的真实对象（代码注释里已承认）
        verify(fileStorageApplicationService, never()).deleteFile(any());
    }

    @Test
    @DisplayName("删除头像：本来就没有头像时不落库")
    void deleteAvatarWithoutAvatarIsNoop() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(null)));

        service.deleteAvatar(USERNAME);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("头像读取：请求者不存在抛 NOT_FOUND")
    void getAvatarContentMissingRequester() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.getAvatarContent(USER_ID, USERNAME));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("头像读取：本人可读")
    void getAvatarContentSelfAllowed() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile(AVATAR_FILE_ID)));
        when(fileStorageApplicationService.downloadFile(AVATAR_FILE_ID))
                .thenReturn(new byte[] {1, 2});

        assertArrayEquals(new byte[] {1, 2}, service.getAvatarContent(USER_ID, USERNAME));
    }

    @Test
    @DisplayName("头像读取：本人但无头像返回 null，不下载")
    void getAvatarContentSelfWithoutAvatar() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(null)));

        assertNull(service.getAvatarContent(USER_ID, USERNAME));
        verify(fileStorageApplicationService, never()).downloadFile(any());
    }

    @Test
    @DisplayName("头像读取：非本人且非管理员抛 FORBIDDEN")
    void getAvatarContentOtherUserForbidden() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        UserRole userRole = new UserRole();
        userRole.setRoleId(3L);
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(userRole));
        Role role = new Role();
        role.setName("操作员");
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role));

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.getAvatarContent(999L, USERNAME));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(fileStorageApplicationService, never()).downloadFile(any());
    }

    @Test
    @DisplayName("头像读取：管理员（角色名 ADMIN，大小写不敏感）可读他人头像")
    void getAvatarContentAdminByEnglishName() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        UserRole userRole = new UserRole();
        userRole.setRoleId(1L);
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(userRole));
        Role role = new Role();
        role.setName("admin");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userProfileRepository.findByUserId(999L))
                .thenReturn(Optional.of(profile(AVATAR_FILE_ID)));
        when(fileStorageApplicationService.downloadFile(AVATAR_FILE_ID)).thenReturn(new byte[] {5});

        assertArrayEquals(new byte[] {5}, service.getAvatarContent(999L, USERNAME));
    }

    @Test
    @DisplayName("头像读取：管理员（角色名 管理员）可读他人头像")
    void getAvatarContentAdminByChineseName() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        UserRole userRole = new UserRole();
        userRole.setRoleId(2L);
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(userRole));
        Role role = new Role();
        role.setName("管理员");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userProfileRepository.findByUserId(999L))
                .thenReturn(Optional.of(profile(AVATAR_FILE_ID)));
        when(fileStorageApplicationService.downloadFile(AVATAR_FILE_ID)).thenReturn(new byte[] {6});

        assertArrayEquals(new byte[] {6}, service.getAvatarContent(999L, USERNAME));
    }

    @Test
    @DisplayName("头像读取：非本人的目标用户没有头像时返回 null")
    void getAvatarContentTargetWithoutAvatar() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        UserRole userRole = new UserRole();
        userRole.setRoleId(1L);
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(userRole));
        Role role = new Role();
        role.setName("ADMIN");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userProfileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertNull(service.getAvatarContent(999L, USERNAME));
    }

    @Test
    @DisplayName("头像上传：用户不存在抛 NOT_FOUND")
    void uploadAvatarMissingUser() {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.uploadAvatar(USERNAME, multipartFile));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(fileStorageApplicationService, never()).uploadFile(any(), any());
    }

    @Test
    @DisplayName("头像上传：无资料行时新建，带上 userId/createTime 与上传返回的文件ID")
    void uploadAvatarCreatesRow() throws Exception {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        FileUploadResponse uploadResponse = new FileUploadResponse();
        uploadResponse.setFileId(AVATAR_FILE_ID);
        when(fileStorageApplicationService.uploadFile(any(), eq(USER_ID)))
                .thenReturn(uploadResponse);
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileDTO dto = service.uploadAvatar(USERNAME, multipartFile);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        UserProfile saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId().longValue());
        assertNotNull(saved.getCreateTime());
        assertEquals(AVATAR_FILE_ID, saved.getAvatarFileId().longValue());
        assertEquals("/api/profile/42/avatar/image", dto.getAvatarUrl());
    }

    @Test
    @DisplayName("头像上传：已有资料行时保留原 createTime")
    void uploadAvatarKeepsCreateTime() throws Exception {
        UserProfile existing = profile(null);
        LocalDateTime original = existing.getCreateTime();
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        FileUploadResponse uploadResponse = new FileUploadResponse();
        uploadResponse.setFileId(AVATAR_FILE_ID);
        when(fileStorageApplicationService.uploadFile(any(), eq(USER_ID)))
                .thenReturn(uploadResponse);
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.uploadAvatar(USERNAME, multipartFile);

        assertEquals(original, existing.getCreateTime());
    }

    @Test
    @DisplayName("头像上传：把上传者ID 与文件一并交给文件服务")
    void uploadAvatarDelegatesToFileService() throws Exception {
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(null)));
        FileUploadResponse uploadResponse = new FileUploadResponse();
        uploadResponse.setFileId(AVATAR_FILE_ID);
        when(fileStorageApplicationService.uploadFile(any(), any())).thenReturn(uploadResponse);
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.uploadAvatar(USERNAME, multipartFile);

        ArgumentCaptor<com.huicang.wise.application.oss.FileUploadRequest> captor =
                ArgumentCaptor.forClass(com.huicang.wise.application.oss.FileUploadRequest.class);
        verify(fileStorageApplicationService).uploadFile(captor.capture(), eq(USER_ID));
        assertEquals(multipartFile, captor.getValue().getFile());
    }

    @Test
    @DisplayName("设置更新：settings 为空 Map 时也会落库（含 userId）")
    void updateUserSettingsWithEmptyMapStillSaves() {
        UserProfile existing = profile(null);
        when(userCoreRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest();
        request.setSettings(new HashMap<>());

        UserSettingsDTO dto = service.updateUserSettings(USERNAME, request);

        verify(userProfileRepository).save(existing);
        assertEquals("张三", dto.getSettings().get("nickname"));
    }
}

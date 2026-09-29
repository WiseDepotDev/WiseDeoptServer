package com.huicang.wise.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.captcha.CaptchaApplicationService;
import com.huicang.wise.application.common.DeleteWithCaptchaRequest;
import com.huicang.wise.application.password.ChangePasswordRequest;
import com.huicang.wise.application.password.PasswordApplicationService;
import com.huicang.wise.application.role.RoleDTO;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.Role;
import com.huicang.wise.domain.auth.UserRole;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.domain.user.UserProfile;
import com.huicang.wise.infrastructure.persistence.repository.auth.KeyAccessAuditLogRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.RoleRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.UserLoginLogRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.UserRoleRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.NfcBadgeRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserCoreRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserProfileRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserSecurityRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 用户应用服务的单元测试：创建/更新/查询/删除、角色映射、密码改签与验证码前置校验。
 *
 * <p>本批把两处现状钉住（都只记录、未修）： ① {@code createUser} **在应用层用 {@code System.nanoTime() + Math.random()}
 * 自造主键** （不是数据库 IDENTITY），断言写成"仓储被调用时实体上就已经有 userId"，把这一点固定下来； ② 第 12 个构造依赖 {@code
 * FileStorageApplicationService} 在本类中**从未被使用**（死依赖）， 本批所有用例都不对它做任何交互。
 */
@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    private static final long USER_ID = 42L;

    @Mock private UserCoreRepository userCoreRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserSecurityRepository userSecurityRepository;
    @Mock private PasswordApplicationService passwordApplicationService;
    @Mock private UserRoleApplicationService userRoleApplicationService;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private NfcBadgeRepository nfcBadgeRepository;
    @Mock private KeyAccessAuditLogRepository keyAccessAuditLogRepository;
    @Mock private UserLoginLogRepository userLoginLogRepository;
    @Mock private CaptchaApplicationService captchaApplicationService;

    private UserApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new UserApplicationService(
                        userCoreRepository,
                        userProfileRepository,
                        userSecurityRepository,
                        passwordApplicationService,
                        userRoleApplicationService,
                        roleRepository,
                        userRoleRepository,
                        nfcBadgeRepository,
                        keyAccessAuditLogRepository,
                        userLoginLogRepository,
                        captchaApplicationService);
    }

    private UserCore user(Short status) {
        UserCore entity = new UserCore();
        entity.setUserId(USER_ID);
        entity.setUsername("zhang");
        entity.setStatus(status);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    private UserProfile profile(Long avatarFileId) {
        UserProfile entity = new UserProfile();
        entity.setNickname("张三");
        entity.setEmail("z@example.com");
        entity.setAvatarFileId(avatarFileId);
        return entity;
    }

    private UserUpdateRequest updateRequest() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUserId(USER_ID);
        return request;
    }

    private void stubUpdateHappyPath(UserCore user, UserProfile profile) {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        // save 是通用夹具：角色映射失败时会在 save 之前抛错，故对这一对桩放宽必要性校验。
        lenient()
                .when(userCoreRepository.save(any(UserCore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient()
                .when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------- 创建 ----------------

    @Test
    @DisplayName("创建用户：用户名重复被拒")
    void createUserRejectsDuplicateUsername() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("zhang");
        when(userCoreRepository.findByUsername("zhang")).thenReturn(Optional.of(user((short) 1)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createUser(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(userCoreRepository, never()).save(any(UserCore.class));
    }

    @Test
    @DisplayName("创建用户：新用户默认启用、未删除、类型 0，资料挂到数据库赋予的ID 上")
    void createUserFillsDefaults() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("zhang");
        request.setNickname("张三");
        request.setEmail("z@example.com");
        when(userCoreRepository.findByUsername("zhang")).thenReturn(Optional.empty());
        when(userCoreRepository.save(any(UserCore.class)))
                .thenAnswer(
                        invocation -> {
                            UserCore toSave = invocation.getArgument(0);
                            assertNull(toSave.getUserId(), "主键必须留给数据库生成");
                            toSave.setUserId(USER_ID);
                            return toSave;
                        });
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO dto = service.createUser(request);

        ArgumentCaptor<UserCore> userCaptor = ArgumentCaptor.forClass(UserCore.class);
        verify(userCoreRepository).save(userCaptor.capture());
        UserCore savedUser = userCaptor.getValue();
        assertEquals("zhang", savedUser.getUsername());
        assertEquals((short) 0, savedUser.getUserType());
        assertEquals((short) 1, savedUser.getStatus());
        assertEquals((short) 0, savedUser.getIsDeleted());
        assertEquals(1L, savedUser.getCreateBy());

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();
        assertEquals(USER_ID, savedProfile.getUserId().longValue());
        assertEquals("张三", savedProfile.getNickname());
        assertEquals(1L, savedProfile.getUpdateBy());

        assertEquals(Boolean.TRUE, dto.getEnabled());
        assertEquals("zhang", dto.getUsername());
    }

    @Test
    @DisplayName("创建用户：不再由应用层自造主键（DDL 中 user_core.user_id 为 auto_increment）")
    void createUserLetsDatabaseAssignPrimaryKey() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("zhang");
        when(userCoreRepository.findByUsername("zhang")).thenReturn(Optional.empty());
        when(userCoreRepository.save(any(UserCore.class)))
                .thenAnswer(
                        invocation -> {
                            UserCore toSave = invocation.getArgument(0);
                            assertNull(toSave.getUserId(), "落库前不该有主键");
                            toSave.setUserId(USER_ID);
                            return toSave;
                        });
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createUser(request);

        verify(userCoreRepository).save(any(UserCore.class));
    }

    // ---------------- 更新 ----------------

    @Test
    @DisplayName("更新用户：用户不存在抛 NOT_FOUND")
    void updateUserMissingUser() {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updateUser(updateRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("更新用户：资料不存在抛 NOT_FOUND")
    void updateUserMissingProfile() {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user((short) 1)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updateUser(updateRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("更新用户：邮箱与昵称被写入（未传的字段保持原值）")
    void updateUserWritesEmailAndNickname() {
        UserCore user = user((short) 1);
        UserProfile profile = profile(null);
        stubUpdateHappyPath(user, profile);
        UserUpdateRequest request = updateRequest();
        request.setEmail("new@example.com");

        service.updateUser(request);

        assertEquals("new@example.com", profile.getEmail());
        assertEquals("张三", profile.getNickname());
        assertNotNull(profile.getUpdateTime());
    }

    @Test
    @DisplayName("更新用户：合法头像 ID 被解析写入")
    void updateUserParsesAvatar() {
        UserCore user = user((short) 1);
        UserProfile profile = profile(null);
        stubUpdateHappyPath(user, profile);
        UserUpdateRequest request = updateRequest();
        request.setAvatar("123");

        service.updateUser(request);

        assertEquals(123L, profile.getAvatarFileId());
    }

    @Test
    @DisplayName("更新用户：非数字头像改为抛 PARAM_ERROR（不再静默清空已有头像）")
    void updateUserRejectsInvalidAvatar() {
        UserCore user = user((short) 1);
        UserProfile profile = profile(999L);
        stubUpdateHappyPath(user, profile);
        UserUpdateRequest request = updateRequest();
        request.setAvatar("不是一个数字");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updateUser(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        assertEquals(999L, profile.getAvatarFileId(), "失败时不得改动原有头像");
    }

    @Test
    @DisplayName("更新用户：enabled=false 把状态改为 0")
    void updateUserDisablesUser() {
        UserCore user = user((short) 1);
        UserProfile profile = profile(null);
        stubUpdateHappyPath(user, profile);
        UserUpdateRequest request = updateRequest();
        request.setEnabled(Boolean.FALSE);

        service.updateUser(request);

        assertEquals((short) 0, user.getStatus());
    }

    @Test
    @DisplayName("更新用户：角色 ADMIN 映射为“管理员”并重建用户角色")
    void updateUserWithAdminRole() {
        UserCore user = user((short) 1);
        UserProfile profile = profile(null);
        stubUpdateHappyPath(user, profile);
        when(roleRepository.findByName("管理员")).thenReturn(Optional.of(new Role()));
        UserUpdateRequest request = updateRequest();
        request.setRole("ADMIN");

        service.updateUser(request);

        verify(userRoleApplicationService).removeUserRoles(USER_ID);
        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(USER_ID, captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("更新用户：未知角色码映射为“访客”")
    void updateUserWithUnknownRoleFallsBackToGuest() {
        UserCore user = user((short) 1);
        UserProfile profile = profile(null);
        stubUpdateHappyPath(user, profile);
        when(roleRepository.findByName("访客")).thenReturn(Optional.of(new Role()));
        UserUpdateRequest request = updateRequest();
        request.setRole("SOMETHING");

        service.updateUser(request);

        verify(roleRepository).findByName("访客");
    }

    @Test
    @DisplayName("更新用户：角色码 USER 也映射为“访客”（现状，非“普通用户”）")
    void updateUserMapsUserRoleToGuest() {
        UserCore user = user((short) 1);
        UserProfile profile = profile(null);
        stubUpdateHappyPath(user, profile);
        when(roleRepository.findByName("访客")).thenReturn(Optional.of(new Role()));
        UserUpdateRequest request = updateRequest();
        request.setRole("USER");

        service.updateUser(request);

        verify(roleRepository).findByName("访客");
    }

    @Test
    @DisplayName("更新用户：角色名不存在时抛 NOT_FOUND，且不得先清空用户角色")
    void updateUserMissingRole() {
        UserCore user = user((short) 1);
        UserProfile profile = profile(null);
        stubUpdateHappyPath(user, profile);
        when(roleRepository.findByName("管理员")).thenReturn(Optional.empty());
        UserUpdateRequest request = updateRequest();
        request.setRole("ADMIN");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updateUser(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(userRoleApplicationService, never()).removeUserRoles(any());
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    // ---------------- 查询 ----------------

    @Test
    @DisplayName("查询用户：不存在抛 NOT_FOUND")
    void getUserMissingUser() {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getUser(USER_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("查询用户：资料缺失抛 NOT_FOUND")
    void getUserMissingProfile() {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user((short) 1)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getUser(USER_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("查询用户：有头像时返回头像接口路径，并带出首个角色码")
    void getUserBuildsAvatarUrlAndRole() {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user((short) 1)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(77L)));
        RoleDTO role = new RoleDTO();
        role.setRoleCode("ADMIN");
        when(userRoleApplicationService.getUserRoles(USER_ID)).thenReturn(List.of(role));

        UserDTO dto = service.getUser(USER_ID);

        assertEquals("/api/profile/42/avatar/image", dto.getAvatar());
        assertEquals("ADMIN", dto.getRole());
        assertEquals(Boolean.TRUE, dto.getEnabled());
    }

    @Test
    @DisplayName("查询用户：无头像时 avatar 为 null")
    void getUserWithoutAvatar() {
        when(userCoreRepository.findById(USER_ID)).thenReturn(Optional.of(user((short) 1)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(null)));

        assertNull(service.getUser(USER_ID).getAvatar());
    }

    @Test
    @DisplayName("按用户名查询：命中返回；不存在抛 NOT_FOUND")
    void getUserByUsername() {
        when(userCoreRepository.findByUsername("zhang")).thenReturn(Optional.of(user((short) 1)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(null)));

        assertEquals("zhang", service.getUserByUsername("zhang").getUsername());

        when(userCoreRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getUserByUsername("nobody"));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ---------------- 列表 ----------------

    @Test
    @DisplayName("用户列表：页码与页大小兜底，按创建时间倒序")
    void listUsersDefaultsPaging() {
        when(userCoreRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.listUsers(0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userCoreRepository).findAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertNotNull(captor.getValue().getSort().getOrderFor("createTime"));
    }

    @Test
    @DisplayName("用户列表：资料缺失时仍返回条目（昵称/邮箱为 null）")
    void listUsersToleratesMissingProfile() {
        when(userCoreRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user((short) 1)), PageRequest.of(0, 10), 1));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        UserPageDTO page = service.listUsers(1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getItems().size());
        assertNull(page.getItems().get(0).getNickname());
        assertEquals(Boolean.TRUE, page.getItems().get(0).getEnabled());
    }

    // ---------------- 删除 ----------------

    @Test
    @DisplayName("删除用户：不存在抛 NOT_FOUND 且不触发任何级联删除")
    void deleteUserMissing() {
        when(userCoreRepository.existsById(USER_ID)).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deleteUser(USER_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(userProfileRepository, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("删除用户：级联清理资料、安全信息、角色、工牌、审计日志、登录日志")
    void deleteUserCascades() {
        when(userCoreRepository.existsById(USER_ID)).thenReturn(true);
        when(userSecurityRepository.existsByUserId(USER_ID)).thenReturn(true);

        service.deleteUser(USER_ID);

        verify(userProfileRepository).deleteByUserId(USER_ID);
        verify(userSecurityRepository).deleteByUserId(USER_ID);
        verify(userRoleApplicationService).removeUserRoles(USER_ID);
        verify(nfcBadgeRepository).deleteByUserId(USER_ID);
        verify(keyAccessAuditLogRepository).deleteByUserId(USER_ID);
        verify(userLoginLogRepository).deleteByUserId(USER_ID);
        verify(userCoreRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("删除用户：没有安全信息时不调用该表的删除")
    void deleteUserSkipsSecurityWhenAbsent() {
        when(userCoreRepository.existsById(USER_ID)).thenReturn(true);
        when(userSecurityRepository.existsByUserId(USER_ID)).thenReturn(false);

        service.deleteUser(USER_ID);

        verify(userSecurityRepository, never()).deleteByUserId(any());
        verify(userCoreRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("带验证码删除：先校验验证码再走删除")
    void deleteUserWithCaptchaDelegates() {
        DeleteWithCaptchaRequest request = new DeleteWithCaptchaRequest();
        request.setId(USER_ID);
        request.setCaptchaId("cid");
        request.setCaptchaCode("1234");
        when(userCoreRepository.existsById(USER_ID)).thenReturn(true);
        when(userSecurityRepository.existsByUserId(USER_ID)).thenReturn(false);

        service.deleteUserWithCaptcha(request);

        verify(captchaApplicationService).enforceCaptcha("cid", "1234");
        verify(userCoreRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("带验证码删除：验证码不通过时不得触达任何仓储（安全约束）")
    void deleteUserWithCaptchaStopsBeforeRepository() {
        DeleteWithCaptchaRequest request = new DeleteWithCaptchaRequest();
        request.setId(USER_ID);
        request.setCaptchaId("cid");
        request.setCaptchaCode("bad");
        doThrow(new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误"))
                .when(captchaApplicationService)
                .enforceCaptcha("cid", "bad");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deleteUserWithCaptcha(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verifyNoInteractions(userCoreRepository);
        verifyNoInteractions(userProfileRepository);
        verifyNoInteractions(nfcBadgeRepository);
    }

    // ---------------- 改密码 ----------------

    @Test
    @DisplayName("改密码：把旧/新密码原样转交密码服务")
    void changePasswordDelegates() {
        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setOldPassword("old-1");
        request.setNewPassword("new-1");

        service.changePassword(USER_ID, request);

        ArgumentCaptor<ChangePasswordRequest> captor =
                ArgumentCaptor.forClass(ChangePasswordRequest.class);
        verify(passwordApplicationService)
                .changePassword(org.mockito.ArgumentMatchers.eq(USER_ID), captor.capture());
        assertEquals("old-1", captor.getValue().getCurrentPassword());
        assertEquals("new-1", captor.getValue().getNewPassword());
    }
}

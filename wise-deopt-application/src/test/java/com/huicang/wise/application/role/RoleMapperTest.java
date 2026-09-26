package com.huicang.wise.application.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.huicang.wise.domain.auth.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link RoleMapper} 单元测试（P2-11 应用层补测）。
 *
 * <p>重点是 {@code mapRoleNameToCode} 的**名称 → 角色码**映射：它把中文角色名收敛成 {@code ADMIN}/{@code USER}
 * 两类码值，是下游判权限的依据；分支不多但**每一条都要固定住**， 尤其是"未识别名称必须退化成 USER"这条默认路径（漏了它就会把未知角色当成管理员）。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-26
 */
class RoleMapperTest {

    private final RoleMapper mapper = new RoleMapper();

    @Test
    @DisplayName("toDTO：null 输入返回 null（不抛 NPE）")
    void toDTONullReturnsNull() {
        assertNull(mapper.toDTO(null));
    }

    @Test
    @DisplayName("toDTO：逐字段照搬实体，不丢字段")
    void toDTOShouldCopyAllFields() {
        Role role = new Role();
        role.setRoleId(11L);
        role.setName("管理员");
        role.setDescription("系统管理员");
        role.setCreateTime(LocalDateTime.of(2026, 9, 1, 9, 0));
        role.setCreateBy(1L);
        role.setUpdateTime(LocalDateTime.of(2026, 9, 2, 10, 0));
        role.setUpdateBy(2L);

        RoleDTO dto = mapper.toDTO(role);

        assertEquals(11L, dto.getRoleId());
        assertEquals("管理员", dto.getName());
        assertEquals("系统管理员", dto.getDescription());
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0), dto.getCreateTime());
        assertEquals(1L, dto.getCreateBy());
        assertEquals(LocalDateTime.of(2026, 9, 2, 10, 0), dto.getUpdateTime());
        assertEquals(2L, dto.getUpdateBy());
    }

    @Test
    @DisplayName("角色码：超级管理员/管理员 → ADMIN")
    void roleCodeSuperAdminAndAdminMapToAdmin() {
        assertEquals("ADMIN", codeOf("超级管理员"));
        assertEquals("ADMIN", codeOf("管理员"));
    }

    @Test
    @DisplayName("角色码：操作员/访客/未知名 → USER（默认路径不能漏，否则未知角色会被当管理员）")
    void roleCodeOthersAndUnknownFallBackToUser() {
        assertEquals("USER", codeOf("操作员"));
        assertEquals("USER", codeOf("访客"));
        assertEquals("USER", codeOf("某个未来新增的角色"), "未识别名称必须退化成 USER");
    }

    @Test
    @DisplayName("角色码：名称为 null → USER（不抛 NPE）")
    void roleCodeNullNameFallsBackToUser() {
        assertEquals("USER", codeOf(null));
    }

    private String codeOf(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return mapper.toDTO(role).getRoleCode();
    }
}

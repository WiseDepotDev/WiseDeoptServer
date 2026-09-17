package com.huicang.wise.infrastructure.converter;

import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.infrastructure.repository.user.UserCoreJpaEntity;
import org.springframework.stereotype.Component;

/**
 * 用户实体转换器
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Component
public class UserEntityConverter {

    /**
     * 将JPA实体转换为领域实体
     *
     * @param jpaEntity JPA实体
     * @return 领域实体
     */
    public UserCore toDomain(UserCoreJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        UserCore domain = new UserCore();
        domain.setUserId(jpaEntity.getUserId());
        domain.setUsername(jpaEntity.getUsername());
        domain.setUserType(jpaEntity.getUserType());
        domain.setOwnerDeviceId(jpaEntity.getOwnerDeviceId());
        domain.setStatus(jpaEntity.getStatus());
        domain.setIsDeleted(jpaEntity.getIsDeleted());
        domain.setCreateBy(jpaEntity.getCreateBy());
        domain.setCreateTime(jpaEntity.getCreateTime());
        domain.setUpdateBy(jpaEntity.getUpdateBy());
        domain.setUpdateTime(jpaEntity.getUpdateTime());
        return domain;
    }

    /**
     * 将领域实体转换为JPA实体
     *
     * @param domain 领域实体
     * @return JPA实体
     */
    public UserCoreJpaEntity toJpa(UserCore domain) {
        if (domain == null) {
            return null;
        }
        UserCoreJpaEntity jpaEntity = new UserCoreJpaEntity();
        jpaEntity.setUserId(domain.getUserId());
        jpaEntity.setUsername(domain.getUsername());
        jpaEntity.setUserType(domain.getUserType());
        jpaEntity.setOwnerDeviceId(domain.getOwnerDeviceId());
        jpaEntity.setStatus(domain.getStatus());
        jpaEntity.setIsDeleted(domain.getIsDeleted());
        jpaEntity.setCreateBy(domain.getCreateBy());
        jpaEntity.setCreateTime(domain.getCreateTime());
        jpaEntity.setUpdateBy(domain.getUpdateBy());
        jpaEntity.setUpdateTime(domain.getUpdateTime());
        return jpaEntity;
    }
}

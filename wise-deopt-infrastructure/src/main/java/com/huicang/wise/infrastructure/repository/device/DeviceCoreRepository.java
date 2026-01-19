package com.huicang.wise.infrastructure.repository.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceCoreRepository extends JpaRepository<DeviceCoreJpaEntity, Long> {

    List<DeviceCoreJpaEntity> findByType(Integer type);

    List<DeviceCoreJpaEntity> findByStatus(Integer status);
}


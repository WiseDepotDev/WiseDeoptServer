package com.huicang.wise.infrastructure.repository.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertHandleLogRepository extends JpaRepository<AlertHandleLogJpaEntity, Long> {

    List<AlertHandleLogJpaEntity> findByEventId(Long eventId);
}


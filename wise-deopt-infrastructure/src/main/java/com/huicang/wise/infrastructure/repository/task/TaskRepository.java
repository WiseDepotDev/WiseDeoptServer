package com.huicang.wise.infrastructure.repository.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 类功能描述：任务仓储接口
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Repository
public interface TaskRepository extends JpaRepository<TaskJpaEntity, Long> {

    List<TaskJpaEntity> findByStatus(Integer status);

    List<TaskJpaEntity> findByTaskType(Integer taskType);

    TaskJpaEntity findFirstByStatusOrderByCreatedAtDesc(Integer status);
}

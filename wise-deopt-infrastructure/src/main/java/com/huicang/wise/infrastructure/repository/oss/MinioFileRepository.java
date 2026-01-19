package com.huicang.wise.infrastructure.repository.oss;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 类功能描述：MinIO文件仓储接口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义文件记录访问接口
 */
public interface MinioFileRepository extends JpaRepository<MinioFileJpaEntity, Long> {
}


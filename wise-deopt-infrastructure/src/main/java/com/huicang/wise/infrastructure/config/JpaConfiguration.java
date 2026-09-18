package com.huicang.wise.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 配置类：配置实体扫描路径与仓储扫描路径。
 *
 * <p>归属说明（P2-10）：{@code @EntityScan} / {@code @EnableJpaRepositories} 属于持久化技术配置， 必须位于
 * infrastructure 层；原先放在 api 层会让入口层直接持有仓储与领域实体的扫描知识， 违反 STD-ARCH-02「依赖只能由外层指向内层」。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
@Configuration
@EntityScan(
        basePackages = {"com.huicang.wise.infrastructure.repository", "com.huicang.wise.domain"})
@EnableJpaRepositories(
        basePackages = {
            "com.huicang.wise.infrastructure.repository",
            "com.huicang.wise.domain.request",
            "com.huicang.wise.domain.repository"
        })
public class JpaConfiguration {}

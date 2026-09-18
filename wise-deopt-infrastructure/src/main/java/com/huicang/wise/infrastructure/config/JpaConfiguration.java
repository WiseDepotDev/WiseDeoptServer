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
 * <p>**扫描范围刻意收窄（2026-02-27，P2-05 准备）**：只托管 {@code com.huicang.wise.domain}（实体）与 {@code
 * com.huicang.wise.infrastructure.persistence.repository}（仓储）。
 *
 * <p>不再包含 {@code com.huicang.wise.infrastructure.repository}。该包内有上一轮 P2-05「先加 PO」留下的 27 个 {@code
 * XxxJpaEntity} 与 5 个仓储，实测在全部模块中**零外部引用**（仅包内自引用）， 但它们的 {@code @Entity} 与 domain 实体映射了**同一批 26
 * 张表**。两套映射同时进入 Hibernate 元模型， 会为同一张表保留两份互相独立的列定义来源：{@code ddl-auto: update} 依据哪一份生成 ALTER 不明确，
 * 属典型的 schema 漂移隐患。这些 PO 属"尚未启用的迁移中间物"，按两步走约定在完成仓储端口化之前 不应被托管，故从扫描中排除；**文件保留**，供 P2-05 续做时复用。
 *
 * <p>同时移除了对本就不存在的 {@code com.huicang.wise.domain.request} 的扫描声明（失效引用）。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
@Configuration
@EntityScan(basePackages = {"com.huicang.wise.domain"})
@EnableJpaRepositories(basePackages = {"com.huicang.wise.infrastructure.persistence.repository"})
public class JpaConfiguration {}

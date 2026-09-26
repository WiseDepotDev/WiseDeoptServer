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
 * <p>不再包含 {@code com.huicang.wise.infrastructure.repository}。该包内有上一轮 P2-05「先加 PO」留下的 **28 个 {@code
 * XxxJpaEntity}**（其中 27 个带 {@code @Entity}）**与 4 个 device 仓储接口 + 1 个活适配器**。它们的 {@code @Entity} 与线上
 * schema 映射了**同一批 27 张表（27/27 全部同名）**。两套映射同时进入 Hibernate 元模型， 会为同一张表保留两份互相独立的列定义来源：{@code ddl-auto:
 * update} 依据哪一份生成 ALTER 不明确， 属典型的 schema 漂移隐患。故从扫描中排除。
 *
 * <p>**第四十四批纠正（本注释此前有两处不准，已按实测更正）**：
 *
 * <ul>
 *   <li>原文写"27 个 XxxJpaEntity 与 5 个仓储" —— 实测是 **28** 个 {@code *JpaEntity}（27 个带 {@code @Entity}，
 *       {@code MonitorRecordJpaEntity} 没有）；另 4 个是 device 仓储接口，第 5 个 {@code UserRepositoryImpl}
 *       不是仓储接口而是**适配器**。
 *   <li>原文写"实测在全部模块中**零外部引用**（仅包内自引用）" —— **在 HEAD 上已不成立**： {@code UserRepositoryImpl} 带
 *       {@code @Repository}，会被 {@code scanBasePackages = "com.huicang.wise"} 组件扫描到，且它是活端口 {@code
 *       com.huicang.wise.infrastructure.persistence.repository.user.UserRepository} 的**唯一实现**（删了会丢
 *       bean）；{@code UserCoreJpaEntity} 也被包外的 {@code converter.UserEntityConverter} 引用。
 *   <li>原文写"同一批 26 张表" —— 实测与线上 schema 重叠 **27** 张（{@code tools/p205-schema-snapshot.sql} 共 34 张表）。
 * </ul>
 *
 * <p>**该隐患已上闸**：{@code tools/p205-legacy-po-gate.js}（第四十四批）把下面四件事变成可执行判据， 反证 4/4：
 * {@code @EntityScan}/{@code @EnableJpaRepositories} 的范围必须**精确**等于本类声明的两个包 · 任何扫描声明里不得出现遗留包 ·
 * **活适配器 {@code UserRepositoryImpl} 必须仍在位且被扫描** · 遗留 {@code *JpaEntity} 文件数只许降。 只靠注释守不住决定（本项目一贯教训）。
 *
 * <p>遗留文件的**删/留**仍是决策项（见《待确认决策清单.md》决策 12）：实测可删 32 个（零引用）， 但 **{@code UserRepositoryImpl} 必须保留** ——
 * 它不是中间物，而是当前生效的适配器。
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

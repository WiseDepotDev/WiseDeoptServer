package com.huicang.wise.infrastructure.persistence;

import jakarta.persistence.Entity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * 领域实体 **DDL 快照导出**（P2-05 第一批）。
 *
 * <p>为什么需要它：P2-05 要把 35 个领域实体上的 ORM
 * 注解（{@code @Entity}/{@code @Table}/{@code @Id}/{@code @GeneratedValue}/{@code @Column}/{@code @Index}
 * 共 445 处）搬到 infrastructure 的 {@code META-INF/orm.xml}， 让 domain 层不再依赖 {@code
 * spring-boot-starter-data-jpa}。这类"映射搬家"最容易出的错是 **列名/类型/可空性/长度/唯一约束/索引静默漂移** ——
 * 它既不编译报错、也不被任何单测发现，只会在生产上与既有数据库对不上。
 *
 * <p>本测试从 **Hibernate 元模型**导出建表 DDL（**不需要任何数据库连接**：只解析元模型，不访问 JDBC 元数据）， 写到 {@code
 * target/p205-schema-snapshot.sql}。再由 {@code tools/p205-schema-gate.js} 与检入的快照逐字节比对： **迁移前后 DDL
 * 必须完全一致**，"搬家"才算被证明是纯机械的。
 *
 * <p>与 Spring Boot 自动配置保持同一口径的三处设置：物理命名策略取 Boot 3 的默认值 {@code
 * CamelCaseToUnderscoresNamingStrategy}、方言取生产用的 {@code MySQLDialect}、开启 SQL 格式化让 diff 可读。 关闭 JDBC
 * 元数据访问（{@code hibernate.boot.allow_jdbc_metadata_access=false}）以便在无数据库的机器上运行。
 *
 * <p>实体口径与 {@code JpaConfiguration} 的 {@code @EntityScan} 一致（{@code com.huicang.wise.domain}）； 若
 * classpath 上存在 {@code META-INF/orm.xml} 则一并加载 —— 那正是 P2-05 的迁移落点，所以
 * **本测试天生同时覆盖"注解"与"XML"两种映射来源**，迁移前后 DDL 一致才算通过。
 *
 * <p>本测试**只导出、不断言**：判定放进门禁，与 {@code p312-string-snapshot.js} 同一分工（"值变即失败"只有一处）。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-24
 */
class DomainSchemaSnapshotTest {

    /** 领域实体包（与 {@code JpaConfiguration} 的 {@code @EntityScan} 同一路径）。 */
    private static final String DOMAIN_PACKAGE = "com.huicang.wise.domain";

    /** 自建 orm.xml（P2-05 迁移落点）在 classpath 上的位置。 */
    private static final String ORM_XML = "META-INF/orm.xml";

    /** 导出产物（相对模块目录；由门禁脚本读取并与检入快照比对）。 */
    private static final String OUTPUT = "target/p205-schema-snapshot.sql";

    /** 生产方言（与 {@code application.yml} 的 {@code database-platform} 同值）。 */
    private static final String DIALECT = "org.hibernate.dialect.MySQLDialect";

    /** Boot 3 默认物理命名策略（未显式写 name 的列用它推导）。 */
    private static final String NAMING_STRATEGY =
            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy";

    @Test
    void exportDomainSchemaSnapshot() throws Exception {
        final List<Class<?>> entities = scanDomainEntities();
        if (entities.isEmpty()) {
            throw new IllegalStateException("未扫描到任何领域实体，快照导出无意义：" + DOMAIN_PACKAGE);
        }

        final StandardServiceRegistry registry =
                new StandardServiceRegistryBuilder()
                        .applySetting("hibernate.dialect", DIALECT)
                        .applySetting("hibernate.physical_naming_strategy", NAMING_STRATEGY)
                        // 关闭 JDBC 元数据访问：本测试要能在没有数据库的机器上跑
                        .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                        .applySetting("hibernate.temp.use_jdbc_metadata_defaults", "false")
                        .build();

        try {
            final MetadataSources sources = new MetadataSources(registry);
            for (final Class<?> entity : entities) {
                sources.addAnnotatedClass(entity);
            }
            final boolean hasOrmXml =
                    Thread.currentThread().getContextClassLoader().getResource(ORM_XML) != null;
            if (hasOrmXml) {
                sources.addResource(ORM_XML);
            }
            final Metadata metadata = sources.buildMetadata();

            final Path out = Paths.get(OUTPUT);
            Files.createDirectories(out.getParent());
            // **必须先删除**：Hibernate 的文件脚本目标是 **append** 模式（为了让 create+drop 能写进同一文件），
            // 不删就会在旧内容后面再追加一份，行数翻倍、门禁永远对不上（实测踩到过：830 → 1660 行）。
            Files.deleteIfExists(out);

            final Map<String, Object> settings = new HashMap<>();
            // 只生成脚本、不碰数据库：scripts.action=create + create-target
            settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
            settings.put(
                    "jakarta.persistence.schema-generation.scripts.create-target", out.toString());
            settings.put("hibernate.hbm2ddl.delimiter", ";");
            settings.put("hibernate.format_sql", "true");
            SchemaManagementToolCoordinator.process(
                    metadata, registry, settings, delayedDropAction -> {});

            final String script = normalise(Files.readString(out, StandardCharsets.UTF_8));
            Files.writeString(out, script, StandardCharsets.UTF_8);
            System.out.println(
                    "[p205-schema] 实体 "
                            + entities.size()
                            + " 个（orm.xml "
                            + (hasOrmXml ? "已加载" : "不存在")
                            + "）→ "
                            + out.toAbsolutePath()
                            + "（"
                            + script.lines().count()
                            + " 行）");
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    /** 扫描 {@code @Entity} 类；按类名排序保证快照**可复现**（否则 DDL 顺序会抖）。 */
    private List<Class<?>> scanDomainEntities() throws ClassNotFoundException {
        final ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        final Set<BeanDefinition> found = provider.findCandidateComponents(DOMAIN_PACKAGE);
        final List<Class<?>> classes = new ArrayList<>(found.size());
        for (final BeanDefinition definition : found) {
            classes.add(Class.forName(definition.getBeanClassName()));
        }
        classes.sort((a, b) -> a.getName().compareTo(b.getName()));
        return classes;
    }

    /** 统一换行与行尾空白：快照只关心 DDL 内容，不关心平台差异。 */
    private static String normalise(String text) {
        final StringBuilder sb = new StringBuilder();
        for (final String line : text.replace("\r\n", "\n").split("\n", -1)) {
            sb.append(line.replaceAll("[ \\t]+$", "")).append('\n');
        }
        final String joined = sb.toString();
        return joined.endsWith("\n\n") ? joined.substring(0, joined.length() - 1) : joined;
    }
}

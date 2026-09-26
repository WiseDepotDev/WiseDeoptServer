package com.huicang.wise.infrastructure.persistence;

import jakarta.persistence.Entity;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.PersistentClass;
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

    /** orm.xml 里"声明一个实体"的字面量（用于推导判据，而不是硬编码实体数）。 */
    private static final String ORM_ENTITY_DECL = "<entity class=\"";

    /**
     * 领域实体映射的**事实记录**（仅注释说明，不作判据 —— 判据见 {@link #readOrmXml()} 的推导）。
     *
     * <p>实测：domain 有 **35 个实体类**（{@code @Entity}），但只生成 **34 张表** —— 其中一个实体与另一实体 共用同一张表。批内一度按"表数
     * 34"写死判据常量，于是**正确的映射被判成错误**（报"应为 34，实际 35"）。 教训：**判据不要由另一个数字反推**。
     */

    /** 导出产物（相对模块目录；由门禁脚本读取并与检入快照比对）。 */
    private static final String OUTPUT = "target/p205-schema-snapshot.sql";

    /** 生产方言（与 {@code application.yml} 的 {@code database-platform} 同值）。 */
    private static final String DIALECT = "org.hibernate.dialect.MySQLDialect";

    /** Boot 3 默认物理命名策略（未显式写 name 的列用它推导）。 */
    private static final String NAMING_STRATEGY =
            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy";

    @Test
    void exportDomainSchemaSnapshot() throws Exception {
        // 注解实体数**可以是 0**：第四十三批把 35 个实体全部搬进 orm.xml 之后，
        // `com.huicang.wise.domain` 里已没有一个 `@Entity`。此处**不能**再把"扫不到注解实体"
        // 当成错误（那正是迁移完成的标志）；映射是否到位由下面的"orm.xml 声明数"断言保证。
        final List<Class<?>> entities = scanDomainEntities();

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
            // **必须显式 addResource**（第四十三批实测结论，与 JPA 规范的直觉相反）。
            //
            // 批内一度把这个调用去掉，想验证"orm.xml 能否被自动发现" —— 结果是**不能**：元模型里只剩 1 个
            // 仍带注解的实体（DeviceCore），33 个走 orm.xml 的实体**全部丢失**，下面的断言当场报
            // "实体数应为 34，实际 1"。所以这件事**不能靠推断**。
            //
            // 对应到生产：Spring Boot 侧同样需要显式注册，已配在 application.yml 的
            // `spring.jpa.mapping-resources: META-INF/orm.xml` —— 它正是把这个 addResource 传给
            // Spring 自己构建的那个 MetadataSources，**机制同一**（但仍属"机制同一"的论证，不是运行期实证）。
            //
            // 下面的"实体数必须等于 EXPECTED_ENTITIES"断言就是这个坑的护栏：任何一种映射来源失效，
            // 它立刻失败，而不是悄悄少 33 张表。
            if (hasOrmXml) {
                sources.addResource(ORM_XML);
            }
            final Metadata metadata = sources.buildMetadata();
            final int mappedEntities = metadata.getEntityBindings().size();
            /**
             * 断言"orm.xml 里声明的实体**全部**进了元模型"。
             *
             * <p>为什么不用硬编码常量：批内我按"表数 34"写了 {@code EXPECTED_ENTITIES = 34}，结果实测是 **35 个实体绑定 / 34
             * 张表**（domain 有 35 个实体类，其中一个与另一实体共用同一张表）—— 常量当场把正确的映射判成错误。**判据应当由来源推导，而不是由另一个数字反推**。
             *
             * <p>这里直接读 orm.xml，数它声明了几个 {@code <entity class=...>}，要求 元模型实体数 ≥ 注解实体数 + orm.xml
             * 声明数。这样只要 orm.xml 没被加载（第四十三批实测： Hibernate 6 的 MetadataSources **不会自动发现**它），断言立刻失败。
             */
            final int declaredInOrmXml = countOccurrences(readOrmXml(), ORM_ENTITY_DECL);
            final int expectedAtLeast = entities.size() + declaredInOrmXml;
            if (mappedEntities < expectedAtLeast) {
                throw new IllegalStateException(
                        "元模型实体数 "
                                + mappedEntities
                                + " < 注解实体 "
                                + entities.size()
                                + " + orm.xml 声明 "
                                + declaredInOrmXml
                                + " = "
                                + expectedAtLeast
                                + "；说明映射来源失效（META-INF/orm.xml "
                                + (hasOrmXml ? "在 classpath 上但未被加载" : "**不存在**")
                                + "）");
            }

            /**
             * **`dynamic-update` 必须由元模型证明，DDL 证明不了它。**
             *
             * <p>`@DynamicUpdate` 只影响 UPDATE 语句的生成方式（只更新变化的列），**不进入建表 DDL** —— 所以 DDL
             * 快照门禁对它天然无感，这是一个只靠快照会**静默漏掉**的迁移风险。
             *
             * <p>期望值同样是**推导**出来的：注解侧带 `@DynamicUpdate` 的实体数 + orm.xml 里
             * `<dynamic-update>true</dynamic-update>` 的声明数。批内一度只数 orm.xml 侧， 于是 `DeviceCore`（注解侧、仍带
             * `@DynamicUpdate`）让断言误报 —— 与"判据不要由单一来源反推"同一教训。
             */
            int annotatedDynamicUpdate = 0;
            for (final Class<?> entity : entities) {
                if (entity.isAnnotationPresent(DynamicUpdate.class)) {
                    annotatedDynamicUpdate++;
                }
            }
            final int declaredDynamicUpdate =
                    annotatedDynamicUpdate
                            + countOccurrences(
                                    readOrmXml(), "<dynamic-update>true</dynamic-update>");
            int actualDynamicUpdate = 0;
            for (final PersistentClass binding : metadata.getEntityBindings()) {
                if (binding.useDynamicUpdate()) {
                    actualDynamicUpdate++;
                }
            }
            if (actualDynamicUpdate != declaredDynamicUpdate) {
                throw new IllegalStateException(
                        "期望 "
                                + declaredDynamicUpdate
                                + " 个实体开启动态更新（注解侧 "
                                + annotatedDynamicUpdate
                                + " + orm.xml 侧 "
                                + (declaredDynamicUpdate - annotatedDynamicUpdate)
                                + "），但元模型里实际只有 "
                                + actualDynamicUpdate
                                + " 个实体的 useDynamicUpdate() 为真（@DynamicUpdate 的 XML 等价物未生效）");
            }

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

    /**
     * 读 classpath 上的 {@code META-INF/orm.xml}（不存在时返回空串）。
     *
     * <p>用途是**推导判据**（而不是硬编码数字）：只要 orm.xml 没被加载进元模型，实体数就会少于 "注解实体 + orm.xml 声明"，测试立刻失败。第四十三批正是靠它发现
     * **Hibernate 6 的 MetadataSources 不会自动发现 orm.xml**。
     */
    private static String readOrmXml() throws IOException {
        try (InputStream in =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(ORM_XML)) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 数一段文本里出现某字面量的次数（{@code Pattern.quote} 保证按字面量匹配）。 */
    private static int countOccurrences(String text, String literal) {
        if (text.isEmpty()) {
            return 0;
        }
        final Matcher matcher = Pattern.compile(Pattern.quote(literal)).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
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

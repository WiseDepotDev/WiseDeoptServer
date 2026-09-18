package com.huicang.wise.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * 架构约束测试（ArchUnit）——把统一标准的依赖方向变成**可执行的构建门禁**。
 *
 * <p>依据《慧仓智控统一开发标准 v1.0》`STD-ARCH-02`、`STD-ARCH-05` 与
 * `wise-depot-plan/CODE_REVIEW_GUIDELINES.md`：
 *
 * <pre>
 * 允许：common ← domain ← infrastructure ← application ← api
 *       application 可依赖 domain / infrastructure / common
 *       api 只依赖 application 与 common
 * 禁止：domain 依赖 api / infrastructure / application
 *       common 依赖任何业务模块
 *       api 直接依赖 infrastructure（须经「领域端口 + 基础设施适配」）
 *       application 依赖 api
 * </pre>
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
@AnalyzeClasses(packages = "com.huicang.wise", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_不应依赖_api = noClasses()
            .that().resideInAPackage("com.huicang.wise.domain..")
            .should().dependOnClassesThat().resideInAPackage("com.huicang.wise.api..")
            .because("领域层必须与入口层解耦（STD-ARCH-02）");

    @ArchTest
    static final ArchRule domain_不应依赖_infrastructure = noClasses()
            .that().resideInAPackage("com.huicang.wise.domain..")
            .should().dependOnClassesThat().resideInAPackage("com.huicang.wise.infrastructure..")
            .because("领域层不得依赖技术实现，应通过端口反转（STD-ARCH-02 / STD-ARCH-05）");

    @ArchTest
    static final ArchRule domain_不应依赖_application = noClasses()
            .that().resideInAPackage("com.huicang.wise.domain..")
            .should().dependOnClassesThat().resideInAPackage("com.huicang.wise.application..")
            .because("依赖只能由外层指向内层（STD-ARCH-02）");

    @ArchTest
    static final ArchRule common_不应依赖业务模块 = noClasses()
            .that().resideInAPackage("com.huicang.wise.common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.huicang.wise.domain..",
                    "com.huicang.wise.infrastructure..",
                    "com.huicang.wise.application..",
                    "com.huicang.wise.api..")
            .because("common 必须是最底层的公共模块（STD-ARCH-02）");

    @ArchTest
    static final ArchRule application_不应依赖_api = noClasses()
            .that().resideInAPackage("com.huicang.wise.application..")
            .should().dependOnClassesThat().resideInAPackage("com.huicang.wise.api..")
            .because("应用层不得反向依赖入口层（STD-ARCH-02）");

    @ArchTest
    static final ArchRule api_不应直接依赖_infrastructure = noClasses()
            .that().resideInAPackage("com.huicang.wise.api..")
            .should().dependOnClassesThat().resideInAPackage("com.huicang.wise.infrastructure..")
            .because("入口层必须经应用服务/领域端口访问基础设施（STD-ARCH-02 / STD-ARCH-05）");

    /**
     * 入口层不得直接访问仓储（必须经应用服务）。
     *
     * <p>已知存量债务以「显式例外 + 任务号」记录，新代码不得再引入：
     * <ul>
     *   <li>{@code DataInitializer}：启动期数据播种，直接使用 12 个仓储（P2-04b 迁移）；</li>
     *   <li>{@code JpaConfiguration}：`@EnableJpaRepositories` 属持久化配置，应在 infrastructure（P2-10）。</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule api_不应直接访问仓储 = noClasses()
            .that().resideInAPackage("com.huicang.wise.api..")
            .and().haveSimpleNameNotContaining("DataInitializer")
            .and().haveSimpleNameNotContaining("JpaConfiguration")
            .should().dependOnClassesThat().resideInAPackage("com.huicang.wise.domain.repository..")
            .because("入口层只做参数适配与转发，持久化访问必须经应用服务（STD-ARCH-02；存量债务见 P2-04b / P2-10）");

    /**
     * 业务代码不得直接构造 {@link RuntimeException}（STD-ERR-01）。
     *
     * <p>业务失败必须使用统一异常类型（{@code BusinessException} + {@link ErrorCode}），
     * 否则会被全局兜底处理器映射为 `SYS-*` 500，把「客户端/业务错误」误报为「系统故障」。
     * 这条规则用于防止 `RuntimeException` 回归。
     */
    @ArchTest
    static final ArchRule 业务代码不应构造_RuntimeException = noClasses()
            .that().resideInAnyPackage(
                    "com.huicang.wise.application..",
                    "com.huicang.wise.domain..",
                    "com.huicang.wise.infrastructure..",
                    "com.huicang.wise.api..")
            .should().callConstructor(RuntimeException.class)
            .because("业务失败必须用 BusinessException + ErrorCode（STD-ERR-01；否则被兜底映射为 500）");

    /**
     * 入口层不得依赖领域实体（STD-NAME-02 / STD-ARCH-05）。
     *
     * <p>领域实体（{@code @Entity}）不得出现在控制器签名或实现中，对外必须使用 DTO；
     * 该规则以「是否标注 JPA {@code @Entity}」作为实体的判定依据，避免误伤领域枚举与值对象。
     *
     * <p>已知存量债务以显式例外记录：{@code DataInitializer}（启动期数据播种需直接构造实体，
     * 共 146 处调用点，随 P2-04b 迁移至 infrastructure 或改为应用服务调用）。
     */
    @ArchTest
    static final ArchRule api_不应依赖领域实体 = noClasses()
            .that().resideInAPackage("com.huicang.wise.api..")
            .and().haveSimpleNameNotContaining("DataInitializer")
            .should().dependOnClassesThat().areAnnotatedWith(jakarta.persistence.Entity.class)
            .because("对外接口必须使用 DTO，禁止暴露领域实体（STD-NAME-02；存量债务见 P2-04b）");

    /**
     * 业务代码不得直接使用 {@code System.out} / {@code System.err}（STD-LOG-02）。
     *
     * <p>日志必须走日志门面（含链路标识与级别控制）；直出标准流无法被日志系统采集与脱敏。
     */
    @ArchTest
    static final ArchRule 业务代码不应直出标准流 = noClasses()
            .that().resideInAnyPackage(
                    "com.huicang.wise.application..",
                    "com.huicang.wise.domain..",
                    "com.huicang.wise.infrastructure..",
                    "com.huicang.wise.api..")
            .should().accessField(System.class, "out")
            .orShould().accessField(System.class, "err")
            .because("业务日志必须走日志门面（STD-LOG-02）");

    /**
     * 领域层不得出现 ORM / Spring 技术细节（STD-ARCH-05）。
     *
     * <p>当前存量违规：`domain` 有 35 个 `@Entity`、36 处 `jakarta.persistence` 与 142 处
     * `org.springframework` 依赖，需按 P2-05「domain 去 ORM（两步走）」迁移后再启用本规则。
     * 保留规则本身是为了记录约束与迁移目标。
     */
    @Test
    @Disabled("存量违规待 P2-05（domain 去 ORM）完成后启用；当前 domain 仍有 35 个 @Entity / 36 处 JPA import")
    void domain_不应依赖_orm_与技术框架() {
        noClasses()
                .that().resideInAPackage("com.huicang.wise.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "javax.persistence..")
                .because("领域模型不得承载持久化技术细节（STD-ARCH-05）")
                .check(new com.tngtech.archunit.core.importer.ClassFileImporter()
                        .importPackages("com.huicang.wise"));
    }
}

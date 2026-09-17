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

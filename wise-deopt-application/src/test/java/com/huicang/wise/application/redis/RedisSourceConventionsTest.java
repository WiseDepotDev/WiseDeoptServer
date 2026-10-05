package com.huicang.wise.application.redis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Redis 相关的**源码约定**门禁（扫源码，不需要容器）。
 *
 * <p>两条规则，各自守一次真实教训（2026-10-05 审计）：
 *
 * <ol>
 *   <li><b>键名不许散落</b>：`"dashboard:kpi"` 这个字面量曾在 **5 个服务里出现 9 次**。 它不会报错 —— 改了其中一处，另一处仍然"成功执行"，
 *       只是清了一个不存在的键：没有异常、没有日志，表现是"看板数字陈旧 1 分钟"这种最难查的 bug。
 *   <li><b>只能有一套缓存机制</b>：`RedisConfig` 里曾并存 Spring 的 `@EnableCaching` + `RedisCacheManager` （1 小时
 *       TTL、无前缀语义、不参与 `auth:permission` 失效）与自研注解。两套都能用，就意味着 一次错误的 import 会得到一个"看起来对、行为完全不同"的缓存。
 * </ol>
 *
 * <p>为什么扫源码而不是用 ArchUnit：ArchUnit 看的是字节码依赖关系，**看不到字符串常量**， 也无法区分"import 了哪种
 * `Cacheable`"。这两条规则恰恰只关乎文本。
 */
class RedisSourceConventionsTest {

    /**
     * 被集中管理的键字面量（前缀也算：`"inspection:task:"` 与 `"inspection:task"` 都要拦）。
     *
     * <p>为什么是白名单而不是"所有含冒号的字符串"：`auth:login:fail:*`、`human:*` 这些 目前各自只有一个 owner（`LoginAttemptGuard`
     * / `HumanVerifyApplicationService`）， 没有散落风险。规则只在**真的散落过**的地方设卡，才不会因为噪音被人关掉。
     */
    private static final List<String> CENTRALIZED_KEY_LITERALS =
            List.of(
                    "\"dashboard:kpi\"",
                    "\"inventory:total\"",
                    "\"inventory:summary:",
                    "\"inspection:task",
                    "\"message:unread");

    /** 允许出现键字面量的文件（键名的家）。 */
    private static final List<String> ALLOWED_FILES =
            List.of("RedisKeys.java", "RedisSourceConventionsTest.java");

    /**
     * 允许直接用 `stringRedisTemplate` 的类：**安全状态**，它们要的是 fail-closed 而不是 fail-open。
     *
     * <p>缓存抖一下应该只是"慢一点/旧一点"，所以缓存的访问统一走 `RedisCacheUtils`（catch+log）。
     * 但这两处不同：读不到人机验证状态就必须**拒绝**（`challenge`/`token` 读不出来等于验证没过），
     * 令牌吊销也一样（读不出来就当作没吊销，但写失败要记下来）。把它们混进"安全包装"里， 等于把一条安全边界的失败方向改成了放行。
     */
    private static final List<String> RAW_TEMPLATE_ALLOWED =
            List.of("HumanVerifyApplicationService.java", "AuthApplicationService.java");

    @Test
    @DisplayName("生产代码里不许出现被集中管理的 Redis 键字面量（一律走 RedisKeys）")
    void redisKeyLiteralsLiveOnlyInRedisKeys() throws IOException {
        Path productionRoot = Paths.get("src/main/java");
        assertTrue(
                Files.isDirectory(productionRoot),
                "找不到 " + productionRoot.toAbsolutePath() + " —— 测试要能定位生产源码（maven 以模块目录为工作目录）");

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(productionRoot)) {
            for (Path file :
                    files.filter(p -> p.toString().endsWith(".java"))
                            .collect(Collectors.toList())) {
                if (ALLOWED_FILES.contains(file.getFileName().toString())) {
                    continue;
                }
                String text = Files.readString(file, StandardCharsets.UTF_8);
                List<String> lines = text.lines().collect(Collectors.toList());
                for (String literal : CENTRALIZED_KEY_LITERALS) {
                    for (int i = 0; i < lines.size(); i++) {
                        if (lines.get(i).contains(literal)) {
                            violations.add(
                                    productionRoot.relativize(file)
                                            + ":"
                                            + (i + 1)
                                            + " 出现了 Redis 键字面量 "
                                            + literal
                                            + " —— 请改用 RedisKeys 里的常量/方法");
                        }
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Redis 键名必须只有一处定义（RedisKeys），否则改动会静默漏掉某个清除点：\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("缓存访问必须走安全包装（应用层不许直接用 stringRedisTemplate）")
    void applicationLayerUsesTheCacheFacadeNotTheRawTemplate() throws IOException {
        Path productionRoot = Paths.get("src/main/java");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(productionRoot)) {
            for (Path file :
                    files.filter(p -> p.toString().endsWith(".java"))
                            .collect(Collectors.toList())) {
                String name = file.getFileName().toString();
                if (RAW_TEMPLATE_ALLOWED.contains(name)) {
                    continue;
                }
                String text = Files.readString(file, StandardCharsets.UTF_8);
                List<String> lines = text.lines().collect(Collectors.toList());
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains("stringRedisTemplate.")) {
                        violations.add(
                                productionRoot.relativize(file)
                                        + ":"
                                        + (i + 1)
                                        + " 直接用了 stringRedisTemplate —— 缓存读写请走 RedisCacheUtils"
                                        + "（它 catch+log，缓存抖动不会把业务写带崩）或对应的 owner 组件");
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "直接用裸 template 做缓存操作时，Redis 一抖就会让**业务写**返回 500 —— "
                        + "失败方向搞反了（缓存问题不该是业务失败）。安全状态（人机验证 / 令牌吊销）例外，"
                        + "它们要 fail-closed，见 RAW_TEMPLATE_ALLOWED：\n"
                        + String.join("\n", violations));
    }

    @Test
    @DisplayName("不许再出现第二套缓存机制（Spring 的 @Cacheable/@CacheEvict）")
    void onlyOneCachingMechanismIsUsed() throws IOException {
        Path productionRoot = Paths.get("src/main/java");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(productionRoot)) {
            for (Path file :
                    files.filter(p -> p.toString().endsWith(".java"))
                            .collect(Collectors.toList())) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                List<String> lines = text.lines().collect(Collectors.toList());
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.contains("org.springframework.cache")) {
                        violations.add(
                                productionRoot.relativize(file)
                                        + ":"
                                        + (i + 1)
                                        + " 引用了 Spring 的缓存注解 —— "
                                        + "本仓的缓存只有一套：com.huicang.wise.infrastructure.redis.annotation.*");
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "两套缓存机制并存时，一次错误的 import 会得到行为完全不同的缓存（TTL/前缀/失效语义都不同），"
                        + "而且看起来一切正常：\n"
                        + String.join("\n", violations));
    }
}

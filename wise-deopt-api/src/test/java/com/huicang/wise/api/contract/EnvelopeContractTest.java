package com.huicang.wise.api.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.huicang.wise.api.config.JpaConfiguration;
import com.huicang.wise.api.controller.UserController;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.application.user.UserCreateRequest;
import com.huicang.wise.application.user.UserDTO;
import com.huicang.wise.common.api.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 三端统一信封契约测试（服务端侧，STD-CONTRACT-01 / STD-TEST-02）。
 *
 * <p>与 APP 端 {@code EnvelopeContractTest}、设备端 {@code test/test_envelope.c} 使用**同一组** 权威样例报文（{@code
 * docs/standards/fixtures/*.json}）与同一份 schema （{@code
 * docs/standards/schemas/envelope.schema.json}），确保三端对信封的理解一致。
 *
 * <p>覆盖：
 *
 * <ul>
 *   <li>成功 / 业务失败 / 系统失败三类样例的结构与字段语义；
 *   <li>样例中的业务码必须在 {@link ErrorCode} 中存在（与对照表联动）；
 *   <li>服务端确实接受「信封信封化请求」并把 {@code payload.data} 交给控制器。
 * </ul>
 *
 * <p>找不到 fixtures 时跳过（裁剪构建环境），不使构建失败。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = JpaConfiguration.class),
            @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.huicang\\.wise\\.(application|infrastructure|domain)\\..*")
        })
class EnvelopeContractTest extends AbstractWebMvcSliceTest {

    /** fixtures 相对路径候选（相对各模块工作目录） */
    private static final String[] FIXTURE_CANDIDATES = {
        "../../docs/standards/fixtures", "../docs/standards/fixtures", "docs/standards/fixtures"
    };

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserApplicationService userApplicationService;

    @MockBean private AuthApplicationService authApplicationService;

    @BeforeEach
    void setUp() {
        when(authApplicationService.validateToken(any())).thenReturn("admin");
    }

    /**
     * 定位 fixtures 目录。
     *
     * @return 目录路径；找不到返回 null
     */
    private Path locateFixtures() {
        for (String candidate : FIXTURE_CANDIDATES) {
            Path path = Paths.get(candidate);
            if (Files.isDirectory(path)) {
                return path.toAbsolutePath();
            }
        }
        return null;
    }

    /**
     * 读取指定样例报文。
     *
     * @param fileName 文件名
     * @return JSON 根节点
     * @throws Exception 读取或解析失败
     */
    private JsonNode loadFixture(String fileName) throws Exception {
        Path dir = locateFixtures();
        assumeTrue(dir != null, "未找到 docs/standards/fixtures，跳过契约校验");
        Path file = dir.resolve(fileName);
        assumeTrue(Files.exists(file), "缺少样例文件: " + fileName);
        return objectMapper.readTree(Files.readString(file, StandardCharsets.UTF_8));
    }

    /**
     * 断言信封结构完整。
     *
     * @param root 样例根节点
     */
    private void assertEnvelopeShape(JsonNode root) {
        JsonNode header = root.get("header");
        JsonNode payload = root.get("payload");

        assertNotNull(header, "缺少 header");
        assertTrue(header.hasNonNull("request_id"), "header.request_id 必填");
        assertTrue(header.hasNonNull("packet_type"), "header.packet_type 必填");
        assertTrue(header.has("timestamp"), "header.timestamp 必填");

        assertNotNull(payload, "缺少 payload");
        assertTrue(payload.hasNonNull("code"), "payload.code 必填");
        assertTrue(payload.hasNonNull("message"), "payload.message 必填");
        assertFalse(payload.has("requestId"), "payload 中不应再出现 requestId（由 header 承载）");
    }

    /**
     * 成功样例：code 为成功码，且不出现 errorCode。
     *
     * @throws Exception 读取失败
     */
    @Test
    @DisplayName("shouldMatchEnvelopeContractWhenSuccessFixture")
    void shouldMatchEnvelopeContractWhenSuccessFixture() throws Exception {
        JsonNode root = loadFixture("envelope-success.json");

        assertEnvelopeShape(root);
        assertEquals(ErrorCode.SUCCESS.getCode(), root.get("payload").get("code").asText());
        assertTrue(root.get("payload").get("data").has("rows"), "分页载荷应为 data.rows");
        // 成功响应：errorCode 必须省略或为 null（STD-CONTRACT-01 第 3 条）
        JsonNode errorCode = root.get("payload").get("errorCode");
        assertTrue(errorCode == null || errorCode.isNull(), "成功响应不应携带非空 errorCode");
    }

    /**
     * 业务失败样例：errorCode 与 code 一致。
     *
     * @throws Exception 读取失败
     */
    @Test
    @DisplayName("shouldCarryErrorCodeWhenBusinessErrorFixture")
    void shouldCarryErrorCodeWhenBusinessErrorFixture() throws Exception {
        JsonNode root = loadFixture("envelope-business-error.json");
        JsonNode payload = root.get("payload");

        assertEnvelopeShape(root);
        assertEquals(
                payload.get("code").asText(),
                payload.get("errorCode").asText(),
                "失败响应 errorCode 应与业务码一致");
        assertTrue(isRegisteredCode(payload.get("code").asText()), "业务码必须存在于 ErrorCode 枚举（与对照表联动）");
    }

    /**
     * 系统失败样例：packet_type 为 UNKNOWN。
     *
     * @throws Exception 读取失败
     */
    @Test
    @DisplayName("shouldUseUnknownPacketTypeWhenSystemErrorFixture")
    void shouldUseUnknownPacketTypeWhenSystemErrorFixture() throws Exception {
        JsonNode root = loadFixture("envelope-system-error.json");

        assertEnvelopeShape(root);
        assertEquals("UNKNOWN", root.get("header").get("packet_type").asText());
        assertTrue(isRegisteredCode(root.get("payload").get("code").asText()));
    }

    /**
     * 判断业务码是否已登记在枚举中。
     *
     * @param code 业务码
     * @return true 表示已登记
     */
    private boolean isRegisteredCode(String code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 所有样例中的业务码都必须在枚举中登记。
     *
     * @throws Exception 读取失败
     */
    @Test
    @DisplayName("shouldRegisterEveryFixtureCodeInEnum")
    void shouldRegisterEveryFixtureCodeInEnum() throws Exception {
        List<String> unknown = new ArrayList<>();
        for (String fixture :
                new String[] {
                    "envelope-success.json",
                    "envelope-business-error.json",
                    "envelope-system-error.json"
                }) {
            String code = loadFixture(fixture).get("payload").get("code").asText();
            if (!isRegisteredCode(code)) {
                unknown.add(fixture + " -> " + code);
            }
        }
        assertTrue(unknown.isEmpty(), "样例中的错误码未在 ErrorCode 中登记：" + unknown);
    }

    /**
     * 服务端应接受信封化请求：payload.data 被解包后绑定到控制器入参。
     *
     * @throws Exception 请求失败
     */
    @Test
    @DisplayName("shouldUnwrapPayloadDataWhenEnvelopedRequest")
    void shouldUnwrapPayloadDataWhenEnvelopedRequest() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        // nickname 为 @NotBlank 必填字段：@Valid 启用后缺省会返回 400（P2-08）
        request.setNickname("测试昵称");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        ObjectNode header = objectMapper.createObjectNode();
        header.put("request_id", "contract-test-req-1");
        header.put("packet_type", "USER_CREATE");
        header.put("timestamp", 1772000000123L);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("data", objectMapper.valueToTree(request));

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.set("header", header);
        envelope.set("payload", payload);

        mockMvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer token")
                                .header("REQUEST-ID", "contract-test-req-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value(ErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.payload.data.username").value("testuser"))
                .andExpect(jsonPath("$.header.request_id").value("contract-test-req-1"));
    }
}

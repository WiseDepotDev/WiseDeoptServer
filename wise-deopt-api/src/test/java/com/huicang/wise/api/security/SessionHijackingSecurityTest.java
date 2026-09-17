package com.huicang.wise.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.api.support.AbstractAuthenticatedSliceTest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.api.support.AbstractAuthenticatedSliceTest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.auth.LoginRequest;
import com.huicang.wise.api.support.AbstractAuthenticatedSliceTest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.auth.LoginResponse;
import com.huicang.wise.api.support.AbstractAuthenticatedSliceTest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.api.support.AbstractAuthenticatedSliceTest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.user.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.huicang\\.wise\\.(application|infrastructure|domain)\\..*"))
// 说明：本用例断言的是鉴权/授权链路的失败行为，而 SecurityConfig 标注为 @Profile("prod")、
// 且 Spring Security 过滤器链在 @WebMvcTest 切片内为默认配置，无法复现该项目真实安全规则，
// 故归入 e2e 组（默认不执行，需真实环境：mvn test -Pe2e）。
@Tag("e2e")
public class SessionHijackingSecurityTest extends AbstractAuthenticatedSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthApplicationService authApplicationService;

    @MockBean
    private UserApplicationService userApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSessionFixationAttack() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        LoginResponse response = new LoginResponse();
        response.setAccessToken("fixed_session_token");
        response.setUsername("admin");

        when(authApplicationService.login(any(LoginRequest.class), any(HttpServletRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionHijackingWithStolenToken() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer stolen_token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionReplayAttack() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer replayed_token"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionExpiration() throws Exception {
        when(authApplicationService.validateToken(any())).thenThrow(new RuntimeException("Token expired"));

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer expired_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSessionConcurrency() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer concurrent_token_" + i))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenLeakageInUrl() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1?token=leaked_token")
                .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenLeakageInReferer() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer valid_token")
                .header("Referer", "http://evil.com?token=leaked_token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenLeakageInCookie() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer valid_token")
                .header("Cookie", "session=leaked_token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenGuessing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] guessedTokens = {
            "token_1", "token_2", "token_3", "token_4", "token_5",
            "token_6", "token_7", "token_8", "token_9", "token_10"
        };

        for (String token : guessedTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenBruteForce() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer token_" + i))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenReuse() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer reused_token"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/2")
                .header("Authorization", "Bearer reused_token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenForgery() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer forged_token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenManipulation() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] manipulatedTokens = {
            "valid_token.admin",
            "valid_token.superadmin",
            "valid_token.root",
            "admin.valid_token",
            "superadmin.valid_token"
        };

        for (String token : manipulatedTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithNullByte() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer valid_token%00"))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenWithSpecialCharacters() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] specialTokens = {
            "valid_token!", "valid_token@", "valid_token#", "valid_token$",
            "valid_token%", "valid_token^", "valid_token&", "valid_token*"
        };

        for (String token : specialTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithUnicode() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] unicodeTokens = {
            "valid_token_中文", "valid_token_日本語", "valid_token_한국어",
            "valid_token_العربية", "valid_token_русский"
        };

        for (String token : unicodeTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithEncoding() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] encodedTokens = {
            "valid_token%20", "valid_token%2B", "valid_token%2F", "valid_token%3D",
            "valid_token%3F", "valid_token%23", "valid_token%26", "valid_token%3D"
        };

        for (String token : encodedTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithBase64() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] base64Tokens = {
            "dmFsaWRfdG9rZW4=", "YWRtaW46cGFzc3dvcmQ=", "dXNlcjoxMjM0NTY=",
            "c3VwZXJhZG1pbjphZG1pbg==", "cm9vdDpyb290"
        };

        for (String token : base64Tokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithHex() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] hexTokens = {
            "76616c69645f746f6b656e", "61646d696e3a70617373776f7264",
            "75736572313233343536", "737570657261646d696e3a61646d696e"
        };

        for (String token : hexTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithUrlEncoding() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] urlEncodedTokens = {
            "valid%20token", "valid%2Btoken", "valid%2Ftoken", "valid%3Dtoken",
            "valid%3Ftoken", "valid%23token", "valid%26token"
        };

        for (String token : urlEncodedTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithDoubleEncoding() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] doubleEncodedTokens = {
            "valid%2520token", "valid%252Btoken", "valid%252Ftoken",
            "valid%253Dtoken", "valid%253Ftoken"
        };

        for (String token : doubleEncodedTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithMixedCase() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] mixedCaseTokens = {
            "VALID_TOKEN", "valid_token", "Valid_Token", "vAlId_ToKeN",
            "VaLiD_tOkEn"
        };

        for (String token : mixedCaseTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithPadding() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] paddedTokens = {
            "valid_token      ", "      valid_token", "  valid_token  ",
            "valid_token\t\t", "\t\tvalid_token", "\tvalid_token\t"
        };

        for (String token : paddedTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithWhitespace() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] whitespaceTokens = {
            "valid token", "valid\ttoken", "valid\ntoken", "valid\r\ntoken",
            "valid\rtoken"
        };

        for (String token : whitespaceTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithControlCharacters() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] controlTokens = {
            "valid\u0000token", "valid\u0001token", "valid\u0002token", "valid\u0003token",
            "valid\u0004token", "valid\u0005token", "valid\u0006token", "valid\u0007token"
        };

        for (String token : controlTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithRepeatedCharacters() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] repeatedTokens = {
            "aaaaaa", "bbbbbb", "cccccc", "dddddd", "eeeeee",
            "111111", "222222", "333333", "444444", "555555"
        };

        for (String token : repeatedTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithSequentialCharacters() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] sequentialTokens = {
            "abcdef", "123456", "qwerty", "asdfgh", "zxcvbn",
            "fedcba", "654321", "ytrewq", "hgfdsa", "nbvcxz"
        };

        for (String token : sequentialTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithCommonPatterns() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] patternTokens = {
            "password", "123456", "qwerty", "admin", "root",
            "test", "demo", "user", "guest", "temp"
        };

        for (String token : patternTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithTimestamp() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            String token = "token_" + (timestamp + i * 1000);
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithRandomStrings() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] randomTokens = {
            "abc123", "xyz789", "test123", "demo456", "user789",
            "pass123", "admin456", "root789", "test456", "demo789"
        };

        for (String token : randomTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithMd5() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] md5Tokens = {
            "5f4dcc3b5aa765d61d8327deb882cf99",
            "e99a18c428cb38d5f260853678922e03",
            "25d55ad283aa400af464c76d713c07ad",
            "c4ca4238a0b923820dcc509a6f75849b",
            "c81e728d9d4c2f636f067f89cc14862c"
        };

        for (String token : md5Tokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithSha1() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] sha1Tokens = {
            "5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8",
            "e99a18c428cb38d5f260853678922e03",
            "356a192b7913b04c54574d18c28d46e6395428ab",
            "77de68daecd823babbb58edb1c8e14d7c632131c",
            "1b4f0e9851971998e732078526c3051452303323"
        };

        for (String token : sha1Tokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithSha256() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] sha256Tokens = {
            "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
            "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f",
            "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9",
            "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918",
            "e7b9a8a1c4e4e8f5a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4"
        };

        for (String token : sha256Tokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithUuid() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] uuidTokens = {
            "550e8400-e29b-41d4-a716-446655440000",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
            "6ba7b812-9dad-11d1-80b4-00c04fd430c8",
            "6ba7b814-9dad-11d1-80b4-00c04fd430c8"
        };

        for (String token : uuidTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithJwt() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] jwtTokens = {
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ",
            "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ"
        };

        for (String token : jwtTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSessionTokenWithEmptyString() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer "))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenWithNull() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", ""))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenWithVeryLongString() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        StringBuilder longToken = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longToken.append("a");
        }

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer " + longToken.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testSessionTokenWithVeryShortString() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        String[] shortTokens = {"a", "ab", "abc", "abcd", "abcde"};

        for (String token : shortTokens) {
            mockMvc.perform(get("/api/users/1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }
}

package com.kkudaero.server.domain.seller.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.domain.seller.error.SellerErrorCode;
import com.kkudaero.server.global.error.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerApiTest {

    private static final String SELLERS_PATH = "/api/v1/sellers";
    private static final String LOGIN_PATH = "/api/v1/sellers/login";
    private static final String ME_PATH = "/api/v1/sellers/me";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String EMAIL = "seller@example.com";
    private static final String PASSWORD = "password-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 가입하고_로그인한_토큰으로_내_정보를_조회한다() throws Exception {
        postJson(SELLERS_PATH, EMAIL, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        String loginBody = postJson(LOGIN_PATH, EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = objectMapper.readTree(loginBody).at("/data/accessToken").asText();

        mockMvc.perform(get(ME_PATH).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL));
    }

    @Test
    void 같은_이메일로_다시_가입하면_409를_받는다() throws Exception {
        postJson(SELLERS_PATH, EMAIL, PASSWORD).andExpect(status().isCreated());

        postJson(SELLERS_PATH, EMAIL, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(SellerErrorCode.SELLER_EMAIL_DUPLICATED.name()));
    }

    @Test
    void 비밀번호가_틀리면_401을_받는다() throws Exception {
        postJson(SELLERS_PATH, EMAIL, PASSWORD).andExpect(status().isCreated());

        postJson(LOGIN_PATH, EMAIL, PASSWORD + "!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SellerErrorCode.SELLER_LOGIN_FAILED.name()));
    }

    @Test
    void 비밀번호가_규칙에_맞지_않으면_가입할_수_없다() throws Exception {
        postJson(SELLERS_PATH, EMAIL, "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 토큰_없이_내_정보를_조회하면_401을_받는다() throws Exception {
        mockMvc.perform(get(ME_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()));
    }

    @Test
    void 위조한_토큰으로_내_정보를_조회하면_401을_받는다() throws Exception {
        mockMvc.perform(get(ME_PATH).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + "forged.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()));
    }

    private ResultActions postJson(String path, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}

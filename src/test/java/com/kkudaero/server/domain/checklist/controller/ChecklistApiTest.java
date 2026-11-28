package com.kkudaero.server.domain.checklist.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.global.error.GlobalErrorCode;
import com.kkudaero.server.support.SellerTestSupport;
import org.junit.jupiter.api.BeforeEach;
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
class ChecklistApiTest {

    private static final String CHECKLIST_STEPS_PATH = "/api/v1/checklist-steps";
    private static final String CHECKLIST_STEP_PATH = "/api/v1/checklist-steps/{step}";
    private static final String CUSTOMS_STEP = "CUSTOMS_ECOMMERCE_CODE";
    private static final int STEP_COUNT = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, "seller@example.com");
    }

    @Test
    void 처음에는_다섯_단계가_순서대로_모두_미완료로_나온다() throws Exception {
        getSteps(accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(STEP_COUNT))
                .andExpect(jsonPath("$.data[0].step").value("BUSINESS_REGISTRATION"))
                .andExpect(jsonPath("$.data[0].order").value(1))
                .andExpect(jsonPath("$.data[0].title").value("사업자등록"))
                .andExpect(jsonPath("$.data[4].step").value("FORWARDING_AGENT"))
                .andExpect(jsonPath("$.data[?(@.isCompleted == true)]").isEmpty())
                .andExpect(jsonPath("$.data[0].completedAt").doesNotExist());
    }

    @Test
    void 앞_단계를_건너뛰고_완료로_표시할_수_있다() throws Exception {
        updateStep(accessToken, CUSTOMS_STEP, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.step").value(CUSTOMS_STEP))
                .andExpect(jsonPath("$.data.isCompleted").value(true))
                .andExpect(jsonPath("$.data.completedAt").exists());

        getSteps(accessToken)
                .andExpect(jsonPath("$.data[0].isCompleted").value(false))
                .andExpect(jsonPath("$.data[2].isCompleted").value(true))
                .andExpect(jsonPath("$.data[2].completedAt").exists());
    }

    @Test
    void 같은_단계를_다시_완료로_표시해도_완료_시각이_바뀌지_않는다() throws Exception {
        String first = updateStep(accessToken, CUSTOMS_STEP, true)
                .andReturn()
                .getResponse()
                .getContentAsString();
        String completedAt = objectMapper.readTree(first).at("/data/completedAt").asText();

        updateStep(accessToken, CUSTOMS_STEP, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedAt").value(completedAt));
    }

    @Test
    void 완료를_되돌릴_수_있고_완료하지_않은_단계를_되돌려도_성공한다() throws Exception {
        updateStep(accessToken, CUSTOMS_STEP, true).andExpect(status().isOk());

        updateStep(accessToken, CUSTOMS_STEP, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCompleted").value(false))
                .andExpect(jsonPath("$.data.completedAt").doesNotExist());
        updateStep(accessToken, CUSTOMS_STEP, false).andExpect(status().isOk());

        getSteps(accessToken).andExpect(jsonPath("$.data[?(@.isCompleted == true)]").isEmpty());
    }

    @Test
    void 다른_셀러의_완료_표시는_내_체크리스트에_보이지_않는다() throws Exception {
        String otherAccessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, "other@example.com");
        updateStep(otherAccessToken, CUSTOMS_STEP, true).andExpect(status().isOk());

        getSteps(accessToken).andExpect(jsonPath("$.data[?(@.isCompleted == true)]").isEmpty());
        getSteps(otherAccessToken).andExpect(jsonPath("$.data[2].isCompleted").value(true));
    }

    @Test
    void 없는_단계나_빈_요청은_400을_받는다() throws Exception {
        updateStep(accessToken, "UNKNOWN", true).andExpect(status().isBadRequest());
        mockMvc.perform(patch(CHECKLIST_STEP_PATH, CUSTOMS_STEP)
                        .header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 토큰_없이_조회하면_401을_받는다() throws Exception {
        mockMvc.perform(get(CHECKLIST_STEPS_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()));
    }

    private ResultActions getSteps(String token) throws Exception {
        return mockMvc.perform(get(CHECKLIST_STEPS_PATH)
                .header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + token));
    }

    private ResultActions updateStep(String token, String step, boolean isCompleted) throws Exception {
        return mockMvc.perform(patch(CHECKLIST_STEP_PATH, step)
                .header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("isCompleted", isCompleted))));
    }
}

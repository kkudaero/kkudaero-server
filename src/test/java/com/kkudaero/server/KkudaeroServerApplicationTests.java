package com.kkudaero.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kkudaero.server.global.error.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KkudaeroServerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void OpenAPI_명세는_인증_없이_조회된다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").exists())
                .andExpect(jsonPath("$.security[0].accessToken").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sellers/me'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/duty-estimates'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sellers/login'].post.security").isEmpty());
    }

    @Test
    void 인증하지_않은_API_요청은_공통_응답_형식의_401을_받는다() throws Exception {
        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()))
                .andExpect(jsonPath("$.message").value(GlobalErrorCode.UNAUTHORIZED.getMessage()));
    }
}

package com.kkudaero.server.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public final class SellerTestSupport {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String PASSWORD = "password-1234";

    private static final String SELLERS_PATH = "/api/v1/sellers";
    private static final String LOGIN_PATH = "/api/v1/sellers/login";
    private static final String ME_PATH = "/api/v1/sellers/me";

    private SellerTestSupport() {
    }

    public static String signUpAndLogin(MockMvc mockMvc, ObjectMapper objectMapper, String email) throws Exception {
        String credentials = objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD));
        mockMvc.perform(post(SELLERS_PATH).contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        String loginBody = mockMvc.perform(
                        post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(loginBody).at("/data/accessToken").asText();
    }

    public static long getSellerId(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken) throws Exception {
        String body = mockMvc.perform(get(ME_PATH).header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).at("/data/id").asLong();
    }
}

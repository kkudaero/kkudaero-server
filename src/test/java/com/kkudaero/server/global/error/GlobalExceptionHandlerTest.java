package com.kkudaero.server.global.error;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kkudaero.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private static final String BUSINESS_PATH = "/business";
    private static final String UNEXPECTED_PATH = "/unexpected";
    private static final String VALIDATION_PATH = "/validation";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 비즈니스_예외는_오류_코드의_상태와_코드로_응답한다() throws Exception {
        mockMvc.perform(get(BUSINESS_PATH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value(GlobalErrorCode.NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 처리하지_못한_예외는_500으로_응답하고_원인을_노출하지_않는다() throws Exception {
        mockMvc.perform(get(UNEXPECTED_PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INTERNAL_SERVER_ERROR.name()))
                .andExpect(jsonPath("$.message").value(GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    @Test
    void 검증에_실패하면_400과_실패한_필드를_응답한다() throws Exception {
        mockMvc.perform(post(VALIDATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.message").value(startsWith("name ")));
    }

    @Test
    void 읽을_수_없는_본문은_400으로_응답한다() throws Exception {
        mockMvc.perform(post(VALIDATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 지원하지_않는_메서드는_405로_응답한다() throws Exception {
        mockMvc.perform(post(BUSINESS_PATH))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.METHOD_NOT_ALLOWED.name()));
    }

    private record NameRequest(@NotBlank String name) {
    }

    @RestController
    private static final class FailingController {

        @GetMapping(BUSINESS_PATH)
        ApiResponse<Void> throwBusinessException() {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND);
        }

        @GetMapping(UNEXPECTED_PATH)
        ApiResponse<Void> throwUnexpectedException() {
            throw new IllegalStateException("내부 원인");
        }

        @PostMapping(VALIDATION_PATH)
        ApiResponse<Void> validate(@Valid @RequestBody NameRequest request) {
            return ApiResponse.success();
        }
    }
}

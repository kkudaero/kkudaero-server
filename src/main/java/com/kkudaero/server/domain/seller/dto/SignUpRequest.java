package com.kkudaero.server.domain.seller.dto;

import com.kkudaero.server.domain.seller.entity.Seller;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @Schema(description = "로그인에 쓸 이메일", example = "seller@example.com")
        @NotBlank @Email @Size(max = Seller.EMAIL_MAX_LENGTH) String email,

        @Schema(description = "비밀번호. 공백 없는 영문·숫자·특수문자 8~64자")
        @NotBlank
        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
        @Pattern(regexp = PASSWORD_PATTERN, message = "영문, 숫자, 특수문자만 쓸 수 있습니다")
        String password
) {

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 64;
    private static final String PASSWORD_PATTERN = "^[\\x21-\\x7E]*$";
}

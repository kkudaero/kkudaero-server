package com.kkudaero.server.global.response;

import com.kkudaero.server.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 응답")
public record ApiResponse<T>(
        @Schema(description = "결과 코드. 성공이면 SUCCESS, 실패면 오류 코드") String code,
        @Schema(description = "결과 설명") String message,
        @Schema(description = "응답 데이터. 실패했거나 돌려줄 값이 없으면 null") T data
) {

    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String SUCCESS_MESSAGE = "요청에 성공했습니다.";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.name(), message, null);
    }
}

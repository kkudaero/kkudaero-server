package com.kkudaero.server.global.error;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 요청을 처리하지 못했습니다.");

    private final HttpStatus status;
    private final String message;

    public static GlobalErrorCode from(HttpStatusCode statusCode) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.status.isSameCodeAs(statusCode))
                .findFirst()
                .orElse(statusCode.is4xxClientError() ? INVALID_REQUEST : INTERNAL_SERVER_ERROR);
    }
}

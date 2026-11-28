package com.kkudaero.server.domain.calculator.error;

import com.kkudaero.server.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CalculatorErrorCode implements ErrorCode {

    EXCHANGE_RATE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "환율을 조회하지 못했습니다. 환율을 직접 입력해 주세요.");

    private final HttpStatus status;
    private final String message;
}

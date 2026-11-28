package com.kkudaero.server.domain.customs.error;

import com.kkudaero.server.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomsErrorCode implements ErrorCode {

    TRACKING_NUMBER_MISSING(HttpStatus.CONFLICT, "주문에 국제 운송장번호가 없습니다. 운송장번호를 먼저 입력해 주세요."),
    CUSTOMS_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "관세청에 아직 통관 진행 정보가 없습니다."),
    CUSTOMS_STATUS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "통관 진행 정보를 조회하지 못했습니다. 잠시 뒤 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;
}

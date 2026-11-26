package com.kkudaero.server.domain.seller.error;

import com.kkudaero.server.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SellerErrorCode implements ErrorCode {

    SELLER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입한 이메일입니다."),
    SELLER_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "셀러를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}

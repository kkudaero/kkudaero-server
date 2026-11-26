package com.kkudaero.server.domain.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;

import com.kkudaero.server.domain.seller.dto.LoginRequest;
import com.kkudaero.server.domain.seller.dto.LoginResponse;
import com.kkudaero.server.domain.seller.dto.SellerResponse;
import com.kkudaero.server.domain.seller.dto.SignUpRequest;
import com.kkudaero.server.domain.seller.entity.Seller;
import com.kkudaero.server.domain.seller.error.SellerErrorCode;
import com.kkudaero.server.domain.seller.repository.SellerRepository;
import com.kkudaero.server.global.error.BusinessException;
import com.kkudaero.server.global.security.AccessToken;
import com.kkudaero.server.global.security.AccessTokenIssuer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    private static final String EMAIL = "seller@example.com";
    private static final String PASSWORD = "password-1234";
    private static final String PASSWORD_HASH = "hashed-password";

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    @InjectMocks
    private SellerService sellerService;

    @Test
    void 가입하면_이메일을_소문자로_정리하고_비밀번호는_해시로_저장한다() {
        given(sellerRepository.existsByEmail(EMAIL)).willReturn(false);
        given(passwordEncoder.encode(PASSWORD)).willReturn(PASSWORD_HASH);
        given(sellerRepository.save(any(Seller.class))).willAnswer(invocation -> invocation.getArgument(0));

        SellerResponse response = sellerService.createSeller(new SignUpRequest("  Seller@Example.com ", PASSWORD));

        ArgumentCaptor<Seller> captor = ArgumentCaptor.forClass(Seller.class);
        verify(sellerRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(captor.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(response.email()).isEqualTo(EMAIL);
    }

    @Test
    void 이미_가입한_이메일이면_가입에_실패한다() {
        given(sellerRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> sellerService.createSeller(new SignUpRequest(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SellerErrorCode.SELLER_EMAIL_DUPLICATED);
        verify(sellerRepository, never()).save(any(Seller.class));
    }

    @Test
    void 비밀번호가_맞으면_액세스_토큰을_돌려준다() {
        AccessToken accessToken = new AccessToken("token", Instant.parse("2026-11-27T01:00:00Z"));
        given(sellerRepository.findByEmail(EMAIL)).willReturn(Optional.of(Seller.create(EMAIL, PASSWORD_HASH)));
        given(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).willReturn(true);
        given(accessTokenIssuer.issue(any())).willReturn(accessToken);

        LoginResponse response = sellerService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.accessToken()).isEqualTo(accessToken.value());
        assertThat(response.expiresAt()).isEqualTo(accessToken.expiresAt());
    }

    @Test
    void 비밀번호가_틀리면_로그인에_실패한다() {
        given(sellerRepository.findByEmail(EMAIL)).willReturn(Optional.of(Seller.create(EMAIL, PASSWORD_HASH)));
        given(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).willReturn(false);

        assertThatThrownBy(() -> sellerService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SellerErrorCode.SELLER_LOGIN_FAILED);
    }

    @Test
    void 가입하지_않은_이메일이면_비밀번호가_틀렸을_때와_같은_오류로_실패한다() {
        given(sellerRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sellerService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SellerErrorCode.SELLER_LOGIN_FAILED);
    }

    @Test
    void 없는_셀러를_조회하면_실패한다() {
        given(sellerRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sellerService.getSeller(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SellerErrorCode.SELLER_NOT_FOUND);
    }
}

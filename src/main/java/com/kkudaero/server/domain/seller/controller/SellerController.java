package com.kkudaero.server.domain.seller.controller;

import com.kkudaero.server.domain.seller.dto.LoginRequest;
import com.kkudaero.server.domain.seller.dto.LoginResponse;
import com.kkudaero.server.domain.seller.dto.SellerResponse;
import com.kkudaero.server.domain.seller.dto.SignUpRequest;
import com.kkudaero.server.domain.seller.service.SellerService;
import com.kkudaero.server.global.response.ApiResponse;
import com.kkudaero.server.global.security.SellerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller", description = "셀러 가입·로그인·내 정보")
public class SellerController {

    private final SellerService sellerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "셀러 가입", description = "이메일과 비밀번호로 가입합니다. 이미 가입한 이메일이면 409를 돌려줍니다.")
    public ApiResponse<SellerResponse> createSeller(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.success(sellerService.createSeller(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "로그인", description = "이메일과 비밀번호가 맞으면 액세스 토큰과 만료 시각을 돌려줍니다.")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(sellerService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "액세스 토큰의 주인인 셀러 정보를 돌려줍니다.")
    public ApiResponse<SellerResponse> getMe(@AuthenticationPrincipal SellerPrincipal principal) {
        return ApiResponse.success(sellerService.getSeller(principal.sellerId()));
    }
}

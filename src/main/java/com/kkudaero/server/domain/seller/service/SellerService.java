package com.kkudaero.server.domain.seller.service;

import java.util.Locale;

import com.kkudaero.server.domain.checklist.repository.ChecklistCompletionRepository;
import com.kkudaero.server.domain.notification.repository.NotificationRepository;
import com.kkudaero.server.domain.order.repository.OrderRepository;
import com.kkudaero.server.domain.seller.dto.LoginRequest;
import com.kkudaero.server.domain.seller.dto.LoginResponse;
import com.kkudaero.server.domain.seller.dto.SellerResponse;
import com.kkudaero.server.domain.seller.dto.SignUpRequest;
import com.kkudaero.server.domain.seller.entity.Seller;
import com.kkudaero.server.domain.seller.error.SellerErrorCode;
import com.kkudaero.server.domain.seller.repository.SellerRepository;
import com.kkudaero.server.global.error.BusinessException;
import com.kkudaero.server.global.security.AccessTokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;
    private final ChecklistCompletionRepository checklistCompletionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;

    @Transactional
    public SellerResponse createSeller(SignUpRequest request) {
        String email = normalizeEmail(request.email());
        if (sellerRepository.existsByEmail(email)) {
            throw new BusinessException(SellerErrorCode.SELLER_EMAIL_DUPLICATED);
        }

        Seller seller = Seller.create(email, passwordEncoder.encode(request.password()));
        return SellerResponse.from(sellerRepository.save(seller));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Seller seller = sellerRepository.findByEmail(normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(SellerErrorCode.SELLER_LOGIN_FAILED));

        return LoginResponse.from(accessTokenIssuer.issue(seller.getId()));
    }

    @Transactional(readOnly = true)
    public SellerResponse getSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .map(SellerResponse::from)
                .orElseThrow(() -> new BusinessException(SellerErrorCode.SELLER_NOT_FOUND));
    }

    @Transactional
    public void deleteSeller(Long sellerId, String password) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(SellerErrorCode.SELLER_NOT_FOUND));
        if (!passwordEncoder.matches(password, seller.getPasswordHash())) {
            throw new BusinessException(SellerErrorCode.SELLER_PASSWORD_MISMATCH);
        }

        notificationRepository.deleteAllBySellerId(sellerId);
        checklistCompletionRepository.deleteAllBySellerId(sellerId);
        orderRepository.deleteAllBySellerId(sellerId);
        sellerRepository.deleteById(sellerId);
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}

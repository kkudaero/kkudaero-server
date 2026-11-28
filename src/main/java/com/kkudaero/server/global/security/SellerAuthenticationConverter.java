package com.kkudaero.server.global.security;

import java.util.List;

import com.kkudaero.server.domain.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SellerAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final SellerRepository sellerRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long sellerId = Long.valueOf(jwt.getSubject());
        if (!sellerRepository.existsById(sellerId)) {
            throw new InvalidBearerTokenException("토큰의 셀러가 없습니다.");
        }
        SellerPrincipal principal = new SellerPrincipal(sellerId);
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, List.of());
    }
}

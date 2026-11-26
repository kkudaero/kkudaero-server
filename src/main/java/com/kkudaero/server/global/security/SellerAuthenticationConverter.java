package com.kkudaero.server.global.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SellerAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        SellerPrincipal principal = new SellerPrincipal(Long.valueOf(jwt.getSubject()));
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, List.of());
    }
}

package com.kkudaero.server.global.security;

import java.time.Instant;

import com.kkudaero.server.global.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AccessToken issue(Long sellerId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
        JwsHeader header = JwsHeader.with(JwtConfig.ALGORITHM).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(sellerId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }
}

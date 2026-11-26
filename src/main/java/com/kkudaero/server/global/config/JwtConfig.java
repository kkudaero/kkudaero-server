package com.kkudaero.server.global.config;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.kkudaero.server.global.security.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    public static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

    private static final String KEY_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(toSecretKey(jwtProperties)));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder.withSecretKey(toSecretKey(jwtProperties))
                .macAlgorithm(ALGORITHM)
                .build();
    }

    private SecretKey toSecretKey(JwtProperties jwtProperties) {
        byte[] secret = Base64.getDecoder().decode(jwtProperties.secret());
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("kkudaero.jwt.secret은 Base64로 인코딩한 32바이트 이상의 값이어야 합니다.");
        }
        return new SecretKeySpec(secret, KEY_ALGORITHM);
    }
}

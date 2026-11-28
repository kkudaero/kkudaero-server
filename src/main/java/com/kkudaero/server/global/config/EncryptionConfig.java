package com.kkudaero.server.global.config;

import com.kkudaero.server.global.util.EncryptionProperties;
import com.kkudaero.server.global.util.PersonalDataCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EncryptionProperties.class)
public class EncryptionConfig {

    @Bean
    public PersonalDataCipher personalDataCipher(EncryptionProperties encryptionProperties) {
        return new PersonalDataCipher(encryptionProperties.key());
    }
}

package com.kkudaero.server.global.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final PersonalDataCipher personalDataCipher;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : personalDataCipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : personalDataCipher.decrypt(dbData);
    }
}

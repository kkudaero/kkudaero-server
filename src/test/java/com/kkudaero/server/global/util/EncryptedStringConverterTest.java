package com.kkudaero.server.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EncryptedStringConverterTest {

    private static final String PERSONAL_CUSTOMS_CODE = "P123456789012";

    @Autowired
    private EncryptedStringConverter converter;

    @Test
    void DB에는_암호문을_넣고_엔티티에는_원문을_돌려준다() {
        String column = converter.convertToDatabaseColumn(PERSONAL_CUSTOMS_CODE);

        assertThat(column).doesNotContain(PERSONAL_CUSTOMS_CODE);
        assertThat(converter.convertToEntityAttribute(column)).isEqualTo(PERSONAL_CUSTOMS_CODE);
    }

    @Test
    void 값이_없으면_그대로_둔다() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}

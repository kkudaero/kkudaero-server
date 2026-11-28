package com.kkudaero.server.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class PersonalDataCipherTest {

    private static final String KEY = base64("test-only-encryption-key-32bytes");
    private static final String OTHER_KEY = base64("other-test-encryption-key-32byte");
    private static final String PLAIN_TEXT = "P123456789012 홍길동";

    private final PersonalDataCipher cipher = new PersonalDataCipher(KEY);

    @Test
    void 암호화한_값을_복호화하면_원문이_나온다() {
        String encrypted = cipher.encrypt(PLAIN_TEXT);

        assertThat(encrypted).doesNotContain(PLAIN_TEXT);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(PLAIN_TEXT);
    }

    @Test
    void 같은_값을_암호화해도_결과는_매번_다르다() {
        assertThat(cipher.encrypt(PLAIN_TEXT)).isNotEqualTo(cipher.encrypt(PLAIN_TEXT));
    }

    @Test
    void 빈_문자열도_암호화하고_복호화한다() {
        assertThat(cipher.decrypt(cipher.encrypt(""))).isEmpty();
    }

    @Test
    void 암호문이_변조되면_복호화에_실패한다() {
        String encrypted = cipher.encrypt(PLAIN_TEXT);
        char last = encrypted.charAt(encrypted.length() - 2);
        String tampered = encrypted.substring(0, encrypted.length() - 2) + (last == 'A' ? 'B' : 'A') + "=";

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 다른_키로는_복호화할_수_없다() {
        String encrypted = cipher.encrypt(PLAIN_TEXT);

        assertThatThrownBy(() -> new PersonalDataCipher(OTHER_KEY).decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 암호화하지_않은_값은_복호화하지_않는다() {
        assertThatThrownBy(() -> cipher.decrypt(PLAIN_TEXT)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 오류_메시지에_원문을_담지_않는다() {
        assertThatThrownBy(() -> cipher.decrypt(PLAIN_TEXT)).hasMessageNotContaining(PLAIN_TEXT);
    }

    @Test
    void 키가_32바이트가_아니면_만들_수_없다() {
        assertThatThrownBy(() -> new PersonalDataCipher(base64("short-key")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PersonalDataCipher("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PersonalDataCipher("not base64!")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 키_앞뒤의_공백과_줄바꿈은_무시한다() {
        String encrypted = new PersonalDataCipher(" " + KEY + "\n").encrypt(PLAIN_TEXT);

        assertThat(cipher.decrypt(encrypted)).isEqualTo(PLAIN_TEXT);
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

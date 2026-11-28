package com.kkudaero.server.global.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class PersonalDataCipher {

    private static final String KEY_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String VERSION_PREFIX = "v1:";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public PersonalDataCipher(String base64Key) {
        this.secretKey = toSecretKey(base64Key);
    }

    public String encrypt(String plainText) {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("개인정보를 암호화하지 못했습니다.", exception);
        }
    }

    public String decrypt(String encrypted) {
        if (!encrypted.startsWith(VERSION_PREFIX)) {
            throw new IllegalStateException("알 수 없는 암호화 형식입니다.");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encrypted.substring(VERSION_PREFIX.length()));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES));
            byte[] plainText = cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("개인정보를 복호화하지 못했습니다.", exception);
        }
    }

    private static SecretKey toSecretKey(String base64Key) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key == null ? "" : base64Key.strip());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("kkudaero.encryption.key는 Base64로 인코딩한 값이어야 합니다.");
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalStateException("kkudaero.encryption.key는 Base64로 인코딩한 32바이트 값이어야 합니다.");
        }
        return new SecretKeySpec(key, KEY_ALGORITHM);
    }
}

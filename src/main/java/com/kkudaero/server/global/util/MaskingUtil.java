package com.kkudaero.server.global.util;

public final class MaskingUtil {

    private static final String MASK = "*";
    private static final int NAME_VISIBLE_PREFIX = 1;
    private static final int PHONE_VISIBLE_SUFFIX = 4;
    private static final int CUSTOMS_CODE_VISIBLE_PREFIX = 1;
    private static final int CUSTOMS_CODE_VISIBLE_SUFFIX = 2;
    private static final String ADDRESS_DELIMITER = " ";

    private MaskingUtil() {
    }

    public static String maskName(String name) {
        return mask(name, NAME_VISIBLE_PREFIX, 0);
    }

    public static String maskPhoneNumber(String phoneNumber) {
        return mask(phoneNumber, 0, PHONE_VISIBLE_SUFFIX);
    }

    public static String maskPersonalCustomsCode(String personalCustomsCode) {
        return mask(personalCustomsCode, CUSTOMS_CODE_VISIBLE_PREFIX, CUSTOMS_CODE_VISIBLE_SUFFIX);
    }

    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        String stripped = address.strip();
        int firstWordEnd = stripped.indexOf(ADDRESS_DELIMITER);
        if (firstWordEnd < 0) {
            return MASK.repeat(stripped.length());
        }
        int maskedLength = stripped.length() - firstWordEnd - ADDRESS_DELIMITER.length();
        return stripped.substring(0, firstWordEnd) + ADDRESS_DELIMITER + MASK.repeat(maskedLength);
    }

    private static String mask(String value, int visiblePrefix, int visibleSuffix) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String stripped = value.strip();
        if (stripped.length() <= visiblePrefix + visibleSuffix) {
            return MASK.repeat(stripped.length());
        }
        int maskedLength = stripped.length() - visiblePrefix - visibleSuffix;
        return stripped.substring(0, visiblePrefix)
                + MASK.repeat(maskedLength)
                + stripped.substring(stripped.length() - visibleSuffix);
    }
}

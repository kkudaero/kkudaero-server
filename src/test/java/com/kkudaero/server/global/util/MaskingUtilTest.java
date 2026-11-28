package com.kkudaero.server.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingUtilTest {

    @Test
    void 이름은_첫_글자만_남긴다() {
        assertThat(MaskingUtil.maskName("홍길동")).isEqualTo("홍**");
    }

    @Test
    void 전화번호는_끝_네_자리만_남긴다() {
        assertThat(MaskingUtil.maskPhoneNumber("010-1234-5678")).isEqualTo("*********5678");
    }

    @Test
    void 개인통관고유부호는_첫_글자와_끝_두_자리만_남긴다() {
        assertThat(MaskingUtil.maskPersonalCustomsCode("P123456789012")).isEqualTo("P**********12");
    }

    @Test
    void 주소는_첫_단어만_남긴다() {
        assertThat(MaskingUtil.maskAddress("서울특별시 강남구 1번길")).isEqualTo("서울특별시 *******");
        assertThat(MaskingUtil.maskAddress("서울특별시")).isEqualTo("*****");
    }

    @Test
    void 남길_글자보다_짧은_값은_모두_가린다() {
        assertThat(MaskingUtil.maskName("홍")).isEqualTo("*");
        assertThat(MaskingUtil.maskPhoneNumber("5678")).isEqualTo("****");
    }

    @Test
    void 값이_없으면_빈_문자열을_돌려준다() {
        assertThat(MaskingUtil.maskName(null)).isEmpty();
        assertThat(MaskingUtil.maskPhoneNumber(" ")).isEmpty();
        assertThat(MaskingUtil.maskAddress(null)).isEmpty();
    }
}

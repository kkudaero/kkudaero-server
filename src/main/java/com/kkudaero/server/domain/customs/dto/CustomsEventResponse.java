package com.kkudaero.server.domain.customs.dto;

import java.time.Instant;

import com.kkudaero.server.infra.unipass.CargoEvent;
import io.swagger.v3.oas.annotations.media.Schema;

public record CustomsEventResponse(
        @Schema(description = "처리 구분", example = "반출신고") String type,
        @Schema(description = "처리 시각(UTC)", nullable = true) Instant processedAt,
        @Schema(description = "장치장 이름") String location,
        @Schema(description = "처리 내용") String detail
) {

    public static CustomsEventResponse from(CargoEvent event) {
        return new CustomsEventResponse(event.type(), event.processedAt(), event.location(), event.detail());
    }
}

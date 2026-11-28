package com.kkudaero.server.infra.unipass;

import java.time.Instant;
import java.util.List;

public record CargoStatus(
        String cargoManagementNumber,
        String clearanceStatus,
        String progressStatus,
        Instant processedAt,
        List<CargoEvent> events,
        Instant fetchedAt
) {
}

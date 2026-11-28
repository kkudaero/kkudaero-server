package com.kkudaero.server.infra.unipass;

import java.time.Instant;

public record CargoEvent(String type, Instant processedAt, String location, String detail) {
}

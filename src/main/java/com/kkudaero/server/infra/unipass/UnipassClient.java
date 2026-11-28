package com.kkudaero.server.infra.unipass;

import java.util.Optional;

public interface UnipassClient {

    Optional<CargoStatus> getCargoStatus(String trackingNumber);
}

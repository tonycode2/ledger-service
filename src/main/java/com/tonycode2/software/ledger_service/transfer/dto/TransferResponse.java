package com.tonycode2.software.ledger_service.transfer.dto;

import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        String type,
        long amount) {

}

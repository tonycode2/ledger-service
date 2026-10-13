package com.tonycode2.software.ledger_service.deposit.dto;

import java.util.UUID;

public record DepositHash(
                String operationType,
                UUID accountId,
                long amount,
                String currency) {

}

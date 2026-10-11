package com.tonycode2.software.ledger_service.ledger.dto;

public record DepositHash(
                String from,
                String to,
                long amount,
                String currency) {

}

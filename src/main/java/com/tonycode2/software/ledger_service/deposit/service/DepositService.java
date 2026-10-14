package com.tonycode2.software.ledger_service.deposit.service;

import com.tonycode2.software.ledger_service.deposit.dto.CreateDeposit;
import com.tonycode2.software.ledger_service.deposit.dto.DepositResponse;

public interface DepositService {
    public DepositResponse deposit(CreateDeposit deposit, String idempotencyId);
}

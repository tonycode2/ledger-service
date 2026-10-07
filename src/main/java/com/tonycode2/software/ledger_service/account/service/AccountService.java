package com.tonycode2.software.ledger_service.account.service;

import java.util.UUID;

import com.tonycode2.software.ledger_service.account.dto.AccountResponse;
import com.tonycode2.software.ledger_service.account.dto.CreateAccountRequest;

public interface AccountService {
    public AccountResponse createAccount(CreateAccountRequest dto);

    public AccountResponse getAccountById(UUID id);
}

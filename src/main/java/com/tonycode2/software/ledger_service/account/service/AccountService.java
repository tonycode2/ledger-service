package com.tonycode2.software.ledger_service.account.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.tonycode2.software.ledger_service.account.dto.AccountResponse;
import com.tonycode2.software.ledger_service.account.dto.CreateAccountRequest;
import com.tonycode2.software.ledger_service.ledger.dto.EntryResponse;

public interface AccountService {
    public AccountResponse createAccount(CreateAccountRequest dto);

    public AccountResponse getAccountById(UUID id);

    public Page<EntryResponse> getEntriesByAccountId(UUID id, int pageNumber, int pageSize, String sortedBy);

}

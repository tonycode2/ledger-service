package com.tonycode2.software.ledger_service.account.service;

import org.springframework.stereotype.Service;

import com.tonycode2.software.ledger_service.account.repository.AccountRepository;

@Service
public class AccountServiceImpl {
    private final AccountRepository repository;

    public AccountServiceImpl(AccountRepository repository) {
        this.repository = repository;
    }

}

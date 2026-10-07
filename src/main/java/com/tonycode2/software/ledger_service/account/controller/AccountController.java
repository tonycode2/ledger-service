package com.tonycode2.software.ledger_service.account.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tonycode2.software.ledger_service.account.dto.AccountResponse;
import com.tonycode2.software.ledger_service.account.dto.CreateAccountRequest;
import com.tonycode2.software.ledger_service.account.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/account")
@Validated
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody @Valid CreateAccountRequest account) {
        AccountResponse response = service.createAccount(account);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
        AccountResponse response = service.getAccountById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}

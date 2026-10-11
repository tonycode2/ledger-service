package com.tonycode2.software.ledger_service.ledger.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tonycode2.software.ledger_service.ledger.dto.CreateDeposit;
import com.tonycode2.software.ledger_service.ledger.dto.DepositResponse;
import com.tonycode2.software.ledger_service.ledger.service.DepositService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/deposits")
public class DepositController {
    private final DepositService service;

    public DepositController(DepositService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DepositResponse> deposit(@RequestBody @Valid CreateDeposit deposit,
            @RequestHeader("X-Idempotency-Key") String idempotencyId) {
        DepositResponse response = service.deposit(deposit, idempotencyId);
        return ResponseEntity.ok(response);
    }
}

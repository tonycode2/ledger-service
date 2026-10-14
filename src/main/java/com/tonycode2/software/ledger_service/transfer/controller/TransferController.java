package com.tonycode2.software.ledger_service.transfer.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tonycode2.software.ledger_service.transfer.dto.CreateTransfer;
import com.tonycode2.software.ledger_service.transfer.dto.TransferResponse;
import com.tonycode2.software.ledger_service.transfer.service.TransferService;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@RequestBody CreateTransfer transfer,
            @RequestHeader("X-Idempotency-Key") String idempotencyId) {
        TransferResponse response = transferService.transfer(transfer, idempotencyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

package com.tonycode2.software.ledger_service.transfer.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tonycode2.software.ledger_service.Utils.RequestHasher;
import com.tonycode2.software.ledger_service.common.exceptions.IdempotencyKeyReusedException;
import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;
import com.tonycode2.software.ledger_service.ledger.repository.EntryRepository;
import com.tonycode2.software.ledger_service.ledger.repository.LedgerTransactionRepository;
import com.tonycode2.software.ledger_service.transfer.dto.CreateTransfer;
import com.tonycode2.software.ledger_service.transfer.dto.TransferHash;
import com.tonycode2.software.ledger_service.transfer.dto.TransferResponse;

@Service
public class TransferServiceImpl implements TransferService {

    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final EntryRepository entryRepository;

    public TransferServiceImpl(LedgerTransactionRepository ledgerTransactionRepository,
            EntryRepository entryRepository) {
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    @Override
    public TransferResponse transfer(CreateTransfer transfer, String idempotencyId) {
        TransferHash hash = new TransferHash(TransactionType.TRANSFER.toString(), transfer.fromAccount(),
                transfer.toAccount(),
                transfer.amount(), transfer.currency());

        String requestHash = RequestHasher.requestHashHelper(hash);

        Optional<LedgerTransaction> ledgerTransactionSameIdempotency = ledgerTransactionRepository
                .findByIdempotencyKey(idempotencyId);

        if (ledgerTransactionSameIdempotency.isEmpty()) {
            LedgerTransaction foundTransaction = ledgerTransactionSameIdempotency.get();
            if (foundTransaction.getRequestHash().equals(requestHash)) {
                return new TransferResponse(foundTransaction.getId(), transfer.fromAccount(), transfer.toAccount(),
                        TransactionType.TRANSFER.toString(), transfer.amount());
            }
            throw new IdempotencyKeyReusedException("The key has already been used with another content");
        }
    }

}

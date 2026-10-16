package com.tonycode2.software.ledger_service.transfer.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.AccountNotFoundException;
import com.tonycode2.software.ledger_service.common.exceptions.IdempotencyKeyReusedException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidCurrencyException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidDuplicateEntryException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidTransferTypeException;
import com.tonycode2.software.ledger_service.ledger.model.Entry;
import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.EntryDirection;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;
import com.tonycode2.software.ledger_service.ledger.repository.EntryRepository;
import com.tonycode2.software.ledger_service.ledger.repository.LedgerTransactionRepository;
import com.tonycode2.software.ledger_service.transfer.dto.CreateTransfer;
import com.tonycode2.software.ledger_service.transfer.dto.TransferHash;
import com.tonycode2.software.ledger_service.transfer.dto.TransferResponse;
import com.tonycode2.software.ledger_service.utils.RequestHasher;

@Service
public class TransferServiceImpl implements TransferService {

    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final EntryRepository entryRepository;
    private final AccountRepository accountRepository;

    public TransferServiceImpl(LedgerTransactionRepository ledgerTransactionRepository,
            EntryRepository entryRepository, AccountRepository accountRepository) {
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    @Override
    public TransferResponse transfer(CreateTransfer transfer, String idempotencyId) {
        // 1) Validaciones baratas, sin tocar la base de datos
        if (transfer.fromAccount().equals(transfer.toAccount())) {
            throw new InvalidDuplicateEntryException("Both accounts are the same");
        }
        String currency = transfer.currency().toUpperCase();

        // 2) Idempotencia
        TransferHash hash = new TransferHash(TransactionType.TRANSFER.toString(), transfer.fromAccount(),
                transfer.toAccount(), transfer.amount(), currency);
        String requestHash = RequestHasher.requestHashHelper(hash);

        Optional<LedgerTransaction> sameKey = ledgerTransactionRepository.findByIdempotencyKey(idempotencyId);
        if (sameKey.isPresent()) {
            LedgerTransaction found = sameKey.get();
            if (found.getRequestHash().equals(requestHash)) {
                return new TransferResponse(found.getId(), transfer.fromAccount(), transfer.toAccount(),
                        TransactionType.TRANSFER.toString(), transfer.amount());
            }
            throw new IdempotencyKeyReusedException("The key has already been used with another content");
        }

        // 3) BLOQUEO: las dos cuentas SIEMPRE en el mismo orden (id menor primero),
        // sin importar quien es origen y quien destino. Asi A->B y B->A no se
        // bloquean mutuamente (sin deadlock): el segundo hilo espera al primero.
        UUID firstId = transfer.fromAccount().compareTo(transfer.toAccount()) < 0
                ? transfer.fromAccount()
                : transfer.toAccount();
        UUID secondId = firstId.equals(transfer.fromAccount()) ? transfer.toAccount() : transfer.fromAccount();

        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account fromAccount = first.getId().equals(transfer.fromAccount()) ? first : second;
        Account toAccount = (fromAccount == first) ? second : first;

        // 4) Reglas de negocio, ya con las filas bloqueadas (el saldo leido es fiable)
        if (!fromAccount.getCurrency().equalsIgnoreCase(currency)
                || !toAccount.getCurrency().equalsIgnoreCase(currency)) {
            throw new InvalidCurrencyException("The transfer currency does not match the accounts");
        }
        if (fromAccount.getType() != AccountType.CUSTOMER || toAccount.getType() != AccountType.CUSTOMER) {
            throw new InvalidTransferTypeException("This transfer is not allowed");
        }

        // 5) Mover el dinero primero (falla pronto con InsufficientFunds), luego
        // persistir
        fromAccount.debit(transfer.amount());
        toAccount.credit(transfer.amount());

        LedgerTransaction savedTransaction = ledgerTransactionRepository
                .save(new LedgerTransaction(idempotencyId, requestHash, TransactionType.TRANSFER, null));
        Entry fromEntry = new Entry(savedTransaction, fromAccount, EntryDirection.DEBIT, transfer.amount());
        Entry toEntry = new Entry(savedTransaction, toAccount, EntryDirection.CREDIT, transfer.amount());
        entryRepository.saveAll(List.of(fromEntry, toEntry));

        return new TransferResponse(savedTransaction.getId(), fromAccount.getId(), toAccount.getId(),
                TransactionType.TRANSFER.toString(), transfer.amount());
    }

}

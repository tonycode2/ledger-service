package com.tonycode2.software.ledger_service.Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.tonycode2.software.ledger_service.ledger.dto.DepositHash;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;

public class RequestHasher {

    public static String requestHashHelper(DepositHash h) {
        String canonical = String.join("|",
                TransactionType.DEPOSIT.toString(),
                h.accountId().toString(),
                String.valueOf(h.amount()),
                h.currency());

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

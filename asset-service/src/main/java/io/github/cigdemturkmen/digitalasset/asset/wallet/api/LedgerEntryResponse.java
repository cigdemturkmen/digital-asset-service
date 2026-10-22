package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import io.github.cigdemturkmen.digitalasset.asset.ledger.LedgerEntry;
import io.github.cigdemturkmen.digitalasset.asset.ledger.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(UUID id, LedgerEntryType type, BigDecimal amount, BigDecimal balanceAfter,
                                  UUID transferId, Instant createdAt) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(entry.getId(), entry.getType(), entry.getAmount(), entry.getBalanceAfter(),
                entry.getTransferId(), entry.getCreatedAt());
    }
}

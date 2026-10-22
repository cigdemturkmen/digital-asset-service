package io.github.cigdemturkmen.digitalasset.asset.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Immutable record of a single balance change. Amounts are always positive; the direction
 * follows from the {@link LedgerEntryType}.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private LedgerEntryType type;

    @Column(nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(updatable = false)
    private UUID transferId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    private LedgerEntry(UUID walletId, LedgerEntryType type, BigDecimal amount,
                        BigDecimal balanceAfter, UUID transferId) {
        this.id = UUID.randomUUID();
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.transferId = transferId;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS); // PostgreSQL stores microseconds
    }

    public static LedgerEntry of(UUID walletId, LedgerEntryType type, BigDecimal amount, BigDecimal balanceAfter) {
        return new LedgerEntry(walletId, type, amount, balanceAfter, null);
    }

    public static LedgerEntry transferLeg(UUID walletId, LedgerEntryType type, BigDecimal amount,
                                          BigDecimal balanceAfter, UUID transferId) {
        return new LedgerEntry(walletId, type, amount, balanceAfter, transferId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public LedgerEntryType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

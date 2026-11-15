package io.github.cigdemturkmen.digitalasset.asset.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * A holder of EURT tokens. The balance is a cached projection of the wallet's ledger entries;
 * it may only change through {@link #credit} and {@link #debit}, which enforce the balance rules.
 */
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Managed by Hibernate only (no setter): bumped on every UPDATE, which fails if another transaction got there first.
    @Version
    private long version;

    /** Required by JPA; not for application code. */
    protected Wallet() {
    }

    public Wallet(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("ownerName must not be blank");
        }
        this.id = UUID.randomUUID();
        this.ownerName = ownerName;
        this.balance = BigDecimal.ZERO;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS); // PostgreSQL stores microseconds
    }

    public void credit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(id, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    // The API validates requests first; this guard also protects callers that don't go through a controller.
    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, got " + amount);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

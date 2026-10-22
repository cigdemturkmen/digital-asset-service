package io.github.cigdemturkmen.digitalasset.asset.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    /** Required by JPA; not for application code. */
    protected Wallet() {
    }

    public Wallet(String ownerName) {
        this.id = UUID.randomUUID();
        this.ownerName = ownerName;
        this.balance = BigDecimal.ZERO;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS); // PostgreSQL stores microseconds
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(id, balance, amount);
        }
        balance = balance.subtract(amount);
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

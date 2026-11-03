package io.github.cigdemturkmen.digitalasset.asset.wallet;

import java.math.BigDecimal;
import java.util.UUID;

/** Business rule violation: a wallet's balance can never go below zero. Knows nothing about HTTP. */
public class InsufficientBalanceException extends RuntimeException {

    private final UUID walletId;
    private final BigDecimal balance;
    private final BigDecimal requested;

    public InsufficientBalanceException(UUID walletId, BigDecimal balance, BigDecimal requested) {
        super("Wallet %s has %s, but %s was requested".formatted(walletId, balance, requested));
        this.walletId = walletId;
        this.balance = balance;
        this.requested = requested;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}

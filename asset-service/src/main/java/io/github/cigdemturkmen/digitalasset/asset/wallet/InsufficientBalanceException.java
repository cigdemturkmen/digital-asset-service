package io.github.cigdemturkmen.digitalasset.asset.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.util.UUID;

@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID walletId, BigDecimal balance, BigDecimal requested) {
        super("Wallet %s has %s, but %s was requested".formatted(walletId, balance, requested));
    }
}

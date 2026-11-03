package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID fromWalletId,
        @NotNull UUID toWalletId,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount) {
}

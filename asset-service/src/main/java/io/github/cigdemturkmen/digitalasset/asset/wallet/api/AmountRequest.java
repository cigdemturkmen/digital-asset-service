package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for mint and burn. {@code @Digits} matches the NUMERIC(19,4) column, so an amount with
 * more than 4 decimals is rejected instead of being silently rounded by PostgreSQL.
 */
public record AmountRequest(@NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount) {
}

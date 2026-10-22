package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import java.math.BigDecimal;

/** Request body for mint and burn. */
public record AmountRequest(BigDecimal amount) {
}

package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(UUID transferId, UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
}

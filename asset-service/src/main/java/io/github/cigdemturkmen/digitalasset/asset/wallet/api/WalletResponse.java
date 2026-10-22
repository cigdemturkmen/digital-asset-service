package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import io.github.cigdemturkmen.digitalasset.asset.wallet.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(UUID id, String ownerName, BigDecimal balance, Instant createdAt) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getOwnerName(), wallet.getBalance(), wallet.getCreatedAt());
    }
}

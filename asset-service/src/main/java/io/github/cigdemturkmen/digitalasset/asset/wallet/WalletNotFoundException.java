package io.github.cigdemturkmen.digitalasset.asset.wallet;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    private final UUID walletId;

    public WalletNotFoundException(UUID walletId) {
        super("Wallet %s not found".formatted(walletId));
        this.walletId = walletId;
    }

    public UUID getWalletId() {
        return walletId;
    }
}

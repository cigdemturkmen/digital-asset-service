package io.github.cigdemturkmen.digitalasset.asset.wallet;

import java.util.UUID;

/** Business rule violation: a transfer needs two different wallets. */
public class SameWalletTransferException extends RuntimeException {

    private final UUID walletId;

    public SameWalletTransferException(UUID walletId) {
        super("Cannot transfer from wallet %s to itself".formatted(walletId));
        this.walletId = walletId;
    }

    public UUID getWalletId() {
        return walletId;
    }
}

package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWalletRequest(@NotBlank @Size(max = 200) String ownerName) {
}

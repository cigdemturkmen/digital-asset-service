package io.github.cigdemturkmen.digitalasset.asset.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}

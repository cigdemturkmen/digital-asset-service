package io.github.cigdemturkmen.digitalasset.asset.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    // Derived query: Spring Data builds
    // "SELECT ... FROM ledger_entries WHERE wallet_id = ? ORDER BY created_at DESC" from the method name.
    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
}

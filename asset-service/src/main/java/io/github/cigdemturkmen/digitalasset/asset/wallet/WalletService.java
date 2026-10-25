package io.github.cigdemturkmen.digitalasset.asset.wallet;

import io.github.cigdemturkmen.digitalasset.asset.ledger.LedgerEntry;
import io.github.cigdemturkmen.digitalasset.asset.ledger.LedgerEntryRepository;
import io.github.cigdemturkmen.digitalasset.asset.ledger.LedgerEntryType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Token operations. Every balance change and its ledger entry are written in the same
 * database transaction, so the ledger always explains the balance.
 */
@Service
public class WalletService {

    private final WalletRepository wallets;
    private final LedgerEntryRepository ledger;

    // Constructor injection: Spring passes the repositories in (no @Autowired needed with a single constructor).
    public WalletService(WalletRepository wallets, LedgerEntryRepository ledger) {
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Transactional
    public Wallet create(String ownerName) {
        return wallets.save(new Wallet(ownerName));
    }

    @Transactional(readOnly = true)
    public Wallet get(UUID walletId) {
        return wallets.findById(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    @Transactional
    public Wallet mint(UUID walletId, BigDecimal amount) {
        Wallet wallet = get(walletId);
        wallet.credit(amount);
        ledger.save(LedgerEntry.of(walletId, LedgerEntryType.MINT, amount, wallet.getBalance()));
        // No wallets.save(...): the wallet is a managed entity, so Hibernate detects the change
        // (dirty checking) and flushes an UPDATE when the transaction commits.
        return wallet;
    }

    @Transactional
    public Wallet burn(UUID walletId, BigDecimal amount) {
        Wallet wallet = get(walletId);
        wallet.debit(amount);
        ledger.save(LedgerEntry.of(walletId, LedgerEntryType.BURN, amount, wallet.getBalance()));
        return wallet;
    }

    @Transactional
    public UUID transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        Wallet from = get(fromWalletId);
        Wallet to = get(toWalletId);
        UUID transferId = UUID.randomUUID();

        from.debit(amount);
        to.credit(amount);

        ledger.save(LedgerEntry.transferLeg(fromWalletId, LedgerEntryType.TRANSFER_OUT, amount, from.getBalance(), transferId));
        ledger.save(LedgerEntry.transferLeg(toWalletId, LedgerEntryType.TRANSFER_IN, amount, to.getBalance(), transferId));
        return transferId;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> history(UUID walletId) {
        get(walletId); // 404 for unknown wallets instead of an empty list
        return ledger.findByWalletIdOrderByCreatedAtDesc(walletId);
    }
}

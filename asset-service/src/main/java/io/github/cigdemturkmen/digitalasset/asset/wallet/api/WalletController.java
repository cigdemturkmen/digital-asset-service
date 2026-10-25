package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import io.github.cigdemturkmen.digitalasset.asset.wallet.Wallet;
import io.github.cigdemturkmen.digitalasset.asset.wallet.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Wallets")
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(summary = "Create a wallet")
    @PostMapping
    public ResponseEntity<WalletResponse> create(@RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.create(request.ownerName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(wallet.getId()).toUri();
        return ResponseEntity.created(location).body(WalletResponse.from(wallet));
    }

    @Operation(summary = "Get a wallet and its balance")
    @GetMapping("/{walletId}")
    public WalletResponse get(@PathVariable UUID walletId) {
        return WalletResponse.from(walletService.get(walletId));
    }

    @Operation(summary = "Mint (issue) new tokens into a wallet")
    @PostMapping("/{walletId}/mint")
    public WalletResponse mint(@PathVariable UUID walletId, @RequestBody AmountRequest request) {
        return WalletResponse.from(walletService.mint(walletId, request.amount()));
    }

    @Operation(summary = "Burn (redeem) tokens from a wallet")
    @PostMapping("/{walletId}/burn")
    public WalletResponse burn(@PathVariable UUID walletId, @RequestBody AmountRequest request) {
        return WalletResponse.from(walletService.burn(walletId, request.amount()));
    }

    @Operation(summary = "List ledger entries, newest first")
    @GetMapping("/{walletId}/ledger")
    public List<LedgerEntryResponse> ledger(@PathVariable UUID walletId) {
        return walletService.history(walletId).stream().map(LedgerEntryResponse::from).toList();
    }
}

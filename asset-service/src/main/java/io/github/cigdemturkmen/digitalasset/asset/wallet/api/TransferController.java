package io.github.cigdemturkmen.digitalasset.asset.wallet.api;

import io.github.cigdemturkmen.digitalasset.asset.wallet.WalletService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final WalletService walletService;

    public TransferController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public TransferResponse transfer(@RequestBody TransferRequest request) {
        UUID transferId = walletService.transfer(request.fromWalletId(), request.toWalletId(), request.amount());
        return new TransferResponse(transferId, request.fromWalletId(), request.toWalletId(), request.amount());
    }
}

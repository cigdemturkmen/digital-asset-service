package io.github.cigdemturkmen.digitalasset.asset.wallet;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests of the entity's own rules: no Spring, no database, so they protect every caller, not just the API. */
class WalletTests {

    @Test
    void ownerNameMustNotBeBlank() {
        assertThatThrownBy(() -> new Wallet(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Wallet(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creditAndDebitRejectNonPositiveAmounts() {
        Wallet wallet = new Wallet("Ayse");
        wallet.credit(new BigDecimal("10"));

        assertThatThrownBy(() -> wallet.credit(new BigDecimal("-50"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(null)).isInstanceOf(IllegalArgumentException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("10");
    }

    @Test
    void debitBeyondBalanceThrowsWithDetailsAndKeepsBalance() {
        Wallet wallet = new Wallet("Ali");
        wallet.credit(new BigDecimal("5"));

        assertThatThrownBy(() -> wallet.debit(new BigDecimal("5.0001")))
                .isInstanceOfSatisfying(InsufficientBalanceException.class, ex -> {
                    assertThat(ex.getWalletId()).isEqualTo(wallet.getId());
                    assertThat(ex.getBalance()).isEqualByComparingTo("5");
                    assertThat(ex.getRequested()).isEqualByComparingTo("5.0001");
                });

        assertThat(wallet.getBalance()).isEqualByComparingTo("5");
    }
}

package io.github.cigdemturkmen.digitalasset.asset.wallet;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end tests through the HTTP API against a real PostgreSQL (started from compose.yaml). */
@SpringBootTest
@AutoConfigureMockMvc
class WalletApiTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void mintTransferAndBurnKeepBalancesAndLedgerConsistent() {
        String ayse = createWallet("Ayse");
        String ali = createWallet("Ali");

        assertThat(post("/api/v1/wallets/" + ayse + "/mint", "{\"amount\": 100.00}")).hasStatusOk();
        assertThat(post("/api/v1/transfers",
                "{\"fromWalletId\": \"%s\", \"toWalletId\": \"%s\", \"amount\": 30}".formatted(ayse, ali)))
                .hasStatusOk()
                .bodyJson().extractingPath("$.transferId").isNotNull();
        assertThat(post("/api/v1/wallets/" + ayse + "/burn", "{\"amount\": 20}")).hasStatusOk();

        assertBalance(ayse, "50");
        assertBalance(ali, "30");

        // Newest first: BURN, TRANSFER_OUT, MINT — and balanceAfter replays the history.
        assertThat(mvc.get().uri("/api/v1/wallets/{id}/ledger", ayse))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[*].type").asArray().containsExactly("BURN", "TRANSFER_OUT", "MINT");
        List<Number> balancesAfter = JsonPath.read(bodyOf(mvc.get().uri("/api/v1/wallets/{id}/ledger", ayse).exchange()),
                "$[*].balanceAfter");
        assertThat(balancesAfter).map(n -> new BigDecimal(n.toString()))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("50"), new BigDecimal("70"), new BigDecimal("100"));
    }

    @Test
    void createReturnsLocationOfNewWallet() {
        MvcTestResult result = post("/api/v1/wallets", "{\"ownerName\": \"Zeynep\"}");

        assertThat(result).hasStatus(HttpStatus.CREATED);
        String id = JsonPath.read(bodyOf(result), "$.id");
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/v1/wallets/" + id);
    }

    @Test
    void burningMoreThanBalanceIsRejectedAndChangesNothing() {
        String wallet = createWallet("Mehmet");
        post("/api/v1/wallets/" + wallet + "/mint", "{\"amount\": 10}");

        assertThat(post("/api/v1/wallets/" + wallet + "/burn", "{\"amount\": 10.01}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);

        assertBalance(wallet, "10");
        assertThat(mvc.get().uri("/api/v1/wallets/{id}/ledger", wallet))
                .bodyJson().extractingPath("$.length()").isEqualTo(1);
    }

    @Test
    void transferWithInsufficientBalanceIsRejectedAndChangesNothing() {
        String from = createWallet("Elif");
        String to = createWallet("Can");
        post("/api/v1/wallets/" + from + "/mint", "{\"amount\": 5}");

        assertThat(post("/api/v1/transfers",
                "{\"fromWalletId\": \"%s\", \"toWalletId\": \"%s\", \"amount\": 6}".formatted(from, to)))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);

        assertBalance(from, "5");
        assertBalance(to, "0");
    }

    @Test
    void unknownWalletReturnsNotFound() {
        assertThat(mvc.get().uri("/api/v1/wallets/{id}", UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    private String createWallet(String ownerName) {
        MvcTestResult result = post("/api/v1/wallets", "{\"ownerName\": \"%s\"}".formatted(ownerName));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(bodyOf(result), "$.id");
    }

    private void assertBalance(String walletId, String expected) {
        MvcTestResult result = mvc.get().uri("/api/v1/wallets/{id}", walletId).exchange();
        assertThat(result).hasStatusOk();
        Number balance = JsonPath.read(bodyOf(result), "$.balance");
        assertThat(new BigDecimal(balance.toString())).isEqualByComparingTo(expected);
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String bodyOf(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}

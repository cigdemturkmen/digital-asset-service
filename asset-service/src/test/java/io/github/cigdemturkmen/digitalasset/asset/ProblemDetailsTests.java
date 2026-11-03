package io.github.cigdemturkmen.digitalasset.asset;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Every error leaves the API as an RFC 9457 problem detail with the right status and machine-readable fields. */
@SpringBootTest
@AutoConfigureMockMvc
class ProblemDetailsTests {

    @Autowired
    private MockMvcTester mvc;

    private String walletId;

    @BeforeEach
    void createWallet() {
        MvcTestResult result = post("/api/v1/wallets", "{\"ownerName\": \"Deniz\"}");
        assertThat(result).hasStatus(HttpStatus.CREATED);
        walletId = JsonPath.read(bodyOf(result), "$.id");
    }

    @Test
    void negativeAmountIsRejectedWithFieldError() {
        String body = assertProblem(post(mintUri(), "{\"amount\": -50}"), HttpStatus.BAD_REQUEST);

        assertThat((String) JsonPath.read(body, "$.detail")).isEqualTo("Request validation failed");
        assertThat((List<String>) JsonPath.read(body, "$.errors[*].field")).containsExactly("amount");
        assertThat((String) JsonPath.read(body, "$.errors[0].message")).isEqualTo("must be greater than 0");
    }

    @Test
    void missingAmountIsRejected() {
        String body = assertProblem(post(mintUri(), "{}"), HttpStatus.BAD_REQUEST);

        assertThat((String) JsonPath.read(body, "$.errors[0].message")).isEqualTo("must not be null");
    }

    @Test
    void moreThanFourDecimalsIsRejectedInsteadOfRounded() {
        String body = assertProblem(post(mintUri(), "{\"amount\": 1.23456}"), HttpStatus.BAD_REQUEST);

        assertThat((List<String>) JsonPath.read(body, "$.errors[*].field")).containsExactly("amount");
        assertBalance("0");
    }

    @Test
    void blankOwnerNameIsRejected() {
        String body = assertProblem(post("/api/v1/wallets", "{\"ownerName\": \"  \"}"), HttpStatus.BAD_REQUEST);

        assertThat((List<String>) JsonPath.read(body, "$.errors[*].field")).containsExactly("ownerName");
    }

    @Test
    void transferListsEveryInvalidField() {
        String body = assertProblem(post("/api/v1/transfers", "{\"amount\": 0}"), HttpStatus.BAD_REQUEST);

        assertThat((List<String>) JsonPath.read(body, "$.errors[*].field"))
                .containsExactly("amount", "fromWalletId", "toWalletId");
    }

    @Test
    void transferToTheSameWalletIsRejectedAndWritesNoLedgerEntries() {
        post(mintUri(), "{\"amount\": 10}");

        String body = assertProblem(post("/api/v1/transfers",
                "{\"fromWalletId\": \"%s\", \"toWalletId\": \"%s\", \"amount\": 1}".formatted(walletId, walletId)),
                HttpStatus.BAD_REQUEST);

        assertThat((String) JsonPath.read(body, "$.title")).isEqualTo("Same wallet transfer");
        assertThat((String) JsonPath.read(body, "$.walletId")).isEqualTo(walletId);
        assertBalance("10");
        assertThat(mvc.get().uri("/api/v1/wallets/{id}/ledger", walletId))
                .bodyJson().extractingPath("$.length()").isEqualTo(1);
    }

    @Test
    void malformedJsonIsBadRequest() {
        assertProblem(post(mintUri(), "{\"amount\":"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidUuidInPathIsBadRequest() {
        assertProblem(post("/api/v1/wallets/not-a-uuid/mint", "{\"amount\": 1}"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownWalletIsNotFoundWithWalletId() {
        UUID unknown = UUID.randomUUID();
        String body = assertProblem(mvc.get().uri("/api/v1/wallets/{id}", unknown).exchange(), HttpStatus.NOT_FOUND);

        assertThat((String) JsonPath.read(body, "$.title")).isEqualTo("Wallet not found");
        assertThat((String) JsonPath.read(body, "$.walletId")).isEqualTo(unknown.toString());
    }

    @Test
    void insufficientBalanceIsUnprocessableWithAmounts() {
        post(mintUri(), "{\"amount\": 3}");

        String body = assertProblem(post("/api/v1/wallets/" + walletId + "/burn", "{\"amount\": 5}"),
                HttpStatus.UNPROCESSABLE_CONTENT);

        assertThat((String) JsonPath.read(body, "$.title")).isEqualTo("Insufficient balance");
        assertThat((String) JsonPath.read(body, "$.walletId")).isEqualTo(walletId);
        assertThat(decimalAt(body, "$.balance")).isEqualByComparingTo("3");
        assertThat(decimalAt(body, "$.requested")).isEqualByComparingTo("5");
        assertThat((String) JsonPath.read(body, "$.instance")).isEqualTo("/api/v1/wallets/" + walletId + "/burn");
    }

    /** Asserts status and problem+json content type, and returns the body for further checks. */
    private String assertProblem(MvcTestResult result, HttpStatus status) {
        assertThat(result).hasStatus(status);
        assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = bodyOf(result);
        assertThat((Integer) JsonPath.read(body, "$.status")).isEqualTo(status.value());
        return body;
    }

    private void assertBalance(String expected) {
        String body = bodyOf(mvc.get().uri("/api/v1/wallets/{id}", walletId).exchange());
        assertThat(decimalAt(body, "$.balance")).isEqualByComparingTo(expected);
    }

    private static BigDecimal decimalAt(String json, String path) {
        Number number = JsonPath.read(json, path);
        return new BigDecimal(number.toString());
    }

    private String mintUri() {
        return "/api/v1/wallets/" + walletId + "/mint";
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String bodyOf(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}

package io.github.cigdemturkmen.digitalasset.asset;

import io.github.cigdemturkmen.digitalasset.asset.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** An unexpected failure becomes a generic 500: the client never sees exception messages, SQL or class names. */
@SpringBootTest
@AutoConfigureMockMvc
class UnexpectedErrorTests {

    @Autowired
    private MockMvcTester mvc;

    // Replaces the real service in this test's application context, so we can make it fail on demand.
    @MockitoBean
    private WalletService walletService;

    @Test
    void unexpectedErrorReturnsGenericProblemWithoutInternals() {
        when(walletService.get(any())).thenThrow(new IllegalStateException("connection to db-host-17 refused"));

        var result = mvc.get().uri("/api/v1/wallets/{id}", UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(result).bodyText()
                .contains("An unexpected error occurred")
                .doesNotContain("db-host-17", "IllegalStateException");
    }
}

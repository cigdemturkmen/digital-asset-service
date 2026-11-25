package io.github.cigdemturkmen.digitalasset.asset.wallet;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires parallel requests at a real server (random port) to prove that concurrent updates of the same wallet are
 * never lost: whatever the mix of successes and conflicts, the balance always equals what its ledger says.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyTests {

    private static final int PARALLEL = 50;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void parallelMintsAreNeverLost() throws Exception {
        String wallet = createWallet("Parallel");

        Map<Integer, Long> statuses = runInParallel(PARALLEL, i -> post("/wallets/" + wallet + "/mint", "{\"amount\": 1}"));

        assertThat(statuses.keySet()).isSubsetOf(200, 409);
        long minted = statuses.getOrDefault(200, 0L);
        assertThat(balanceOf(wallet)).isEqualByComparingTo(BigDecimal.valueOf(minted));
        assertThat(ledgerSumOf(wallet)).isEqualByComparingTo(BigDecimal.valueOf(minted));
    }

    @Test
    void parallelBurnsCannotSpendTheSameBalanceTwice() throws Exception {
        String wallet = createWallet("DoubleSpend");
        post("/wallets/" + wallet + "/mint", "{\"amount\": 100}");

        Map<Integer, Long> statuses = runInParallel(10, i -> post("/wallets/" + wallet + "/burn", "{\"amount\": 60}"));

        // Exactly one burn of 60 fits into 100; the others see the new balance and fail, or lose the race.
        assertThat(statuses.get(200)).isEqualTo(1L);
        assertThat(statuses.keySet()).isSubsetOf(200, 409, 422);
        assertThat(balanceOf(wallet)).isEqualByComparingTo("40");
        assertThat(ledgerSumOf(wallet)).isEqualByComparingTo("40");
    }

    @Test
    void transfersInOppositeDirectionsKeepTotalsAndDoNotDeadlock() throws Exception {
        String a = createWallet("A");
        String b = createWallet("B");
        post("/wallets/" + a + "/mint", "{\"amount\": 100}");
        post("/wallets/" + b + "/mint", "{\"amount\": 100}");

        Map<Integer, Long> statuses = runInParallel(PARALLEL, i -> {
            String from = i % 2 == 0 ? a : b;
            String to = i % 2 == 0 ? b : a;
            return post("/transfers", "{\"fromWalletId\": \"%s\", \"toWalletId\": \"%s\", \"amount\": 1}".formatted(from, to));
        });

        assertThat(statuses.keySet()).isSubsetOf(200, 409); // a deadlock would surface as 500
        assertThat(balanceOf(a).add(balanceOf(b))).isEqualByComparingTo("200");
        assertThat(balanceOf(a)).isEqualByComparingTo(ledgerSumOf(a));
        assertThat(balanceOf(b)).isEqualByComparingTo(ledgerSumOf(b));
    }

    /** Starts all tasks at the same moment and counts the HTTP status codes they returned. */
    private Map<Integer, Long> runInParallel(int count, Function<Integer, HttpResponse<String>> task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < count; i++) {
                int n = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.apply(n);
                }));
            }
            start.countDown();
        }
        List<Integer> statuses = new ArrayList<>();
        for (Future<HttpResponse<String>> future : futures) {
            statuses.add(future.get().statusCode());
        }
        return statuses.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
    }

    /** Replays the ledger: mints and incoming transfers add, burns and outgoing transfers subtract. */
    private BigDecimal ledgerSumOf(String wallet) {
        String body = get("/wallets/" + wallet + "/ledger").body();
        List<String> types = JsonPath.read(body, "$[*].type");
        List<Number> amounts = JsonPath.read(body, "$[*].amount");
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < types.size(); i++) {
            BigDecimal amount = new BigDecimal(amounts.get(i).toString());
            sum = switch (types.get(i)) {
                case "MINT", "TRANSFER_IN" -> sum.add(amount);
                default -> sum.subtract(amount);
            };
        }
        return sum;
    }

    private BigDecimal balanceOf(String wallet) {
        Number balance = JsonPath.read(get("/wallets/" + wallet).body(), "$.balance");
        return new BigDecimal(balance.toString());
    }

    private String createWallet(String ownerName) {
        HttpResponse<String> response = post("/wallets", "{\"ownerName\": \"%s\"}".formatted(ownerName));
        assertThat(response.statusCode()).isEqualTo(201);
        return JsonPath.read(response.body(), "$.id");
    }

    private HttpResponse<String> post(String path, String json) {
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    private HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + "/api/v1" + path);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}

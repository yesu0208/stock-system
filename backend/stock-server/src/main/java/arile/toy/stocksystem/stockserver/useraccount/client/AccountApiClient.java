package arile.toy.stocksystem.stockserver.useraccount.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountApiClient {

    private final RestClient restClient;

    @Value("${account-api.base-url}")
    private String baseUrl;

    public boolean reserveCash(String username, long amount) {
        return post("/internal/accounts/" + username + "/reserve-cash",
                Map.of("amount", amount));
    }

    public boolean refundReservedCash(String username, long amount) {
        return post("/internal/accounts/" + username + "/refund-cash",
                Map.of("amount", amount));
    }

    public boolean reserveStock(String username, String stockCode, int quantity) {
        return post("/internal/accounts/" + username + "/reserve-stock",
                Map.of("stockCode", stockCode, "quantity", quantity));
    }

    public boolean refundReservedStock(String username, String stockCode, int quantity) {
        return post("/internal/accounts/" + username + "/refund-stock",
                Map.of("stockCode", stockCode, "quantity", quantity));
    }

    public void settle(Set<String> usernames) {
        try {
            restClient.post()
                    .uri(baseUrl + "/internal/accounts/settle")
                    .body(Map.of("usernames", usernames))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Account settle API call failed.", e);
        }
    }

    private boolean post(String path, Map<String, Object> body) {
        try {
            BalanceCommandResponse response = restClient.post()
                    .uri(baseUrl + path)
                    .body(body)
                    .retrieve()
                    .body(BalanceCommandResponse.class);

            return response != null && response.success();
        } catch (RestClientException e) {
            log.error("Account API call failed. path={}", path, e);
            return false;
        }
    }

    private record BalanceCommandResponse(boolean success) {
    }
}

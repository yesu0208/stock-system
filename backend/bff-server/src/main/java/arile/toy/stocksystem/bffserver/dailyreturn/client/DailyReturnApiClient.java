package arile.toy.stocksystem.bffserver.dailyreturn.client;

import arile.toy.stocksystem.bffserver.dailyreturn.dto.DailyReturnHistoryResponse;
import arile.toy.stocksystem.bffserver.history.client.HistoryUriBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReturnApiClient {

    private final RestClient restClient;

    @Value("${account-api.base-url}")
    private String baseUrl;

    public DailyReturnHistoryResponse getHistory(String username, LocalDate from, LocalDate to, int page, int size) {
        try {
            URI uri = HistoryUriBuilder.build(
                    baseUrl, "/internal/returns/" + username + "/history", from, to, page, size);

            return restClient.get().uri(uri).retrieve().body(DailyReturnHistoryResponse.class);
        } catch (RestClientException e) {
            log.warn("Daily return history API call failed. username={}", username, e);
            return null;
        }
    }
}

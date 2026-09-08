package arile.toy.stocksystem.bffserver.dailyreturn.client;

import arile.toy.stocksystem.bffserver.dailyreturn.dto.DailyReturnHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
            StringBuilder uri = new StringBuilder(
                    baseUrl + "/internal/returns/" + username + "/history?page=" + page + "&size=" + size);
            if (from != null) uri.append("&from=").append(from);
            if (to != null) uri.append("&to=").append(to);

            return restClient.get().uri(uri.toString()).retrieve().body(DailyReturnHistoryResponse.class);
        } catch (RestClientException e) {
            log.warn("Daily return history API call failed. username={}", username, e);
            return null;
        }
    }
}

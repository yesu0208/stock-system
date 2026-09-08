package arile.toy.stocksystem.bffserver.autoorder.client;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderHistoryItem;
import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoOrderHistoryApiClient {

    private final RestClient restClient;

    @Value("${stock-api.base-url}")
    private String baseUrl;

    public HistoryPageResponse<AutoOrderHistoryItem> getHistory(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/auto-orders/" + username + "/history", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<AutoOrderHistoryItem> getCancels(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/auto-orders/" + username + "/cancels", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<AutoOrderHistoryItem> getUnfilled(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/auto-orders/" + username + "/unfilled", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<AutoOrderHistoryItem> getTriggered(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/auto-orders/" + username + "/triggered", stockCode, from, to, page, size);
    }

    private HistoryPageResponse<AutoOrderHistoryItem> get(
            String path, String stockCode, Instant from, Instant to, int page, int size) {
        try {
            StringBuilder uri = new StringBuilder(baseUrl + path + "?page=" + page + "&size=" + size);
            if (stockCode != null) uri.append("&stockCode=").append(stockCode);
            if (from != null) uri.append("&from=").append(from);
            if (to != null) uri.append("&to=").append(to);

            return restClient.get().uri(uri.toString()).retrieve()
                    .body(new ParameterizedTypeReference<HistoryPageResponse<AutoOrderHistoryItem>>() {});
        } catch (RestClientException e) {
            log.warn("Auto order history API call failed. path={}", path, e);
            return null;
        }
    }
}

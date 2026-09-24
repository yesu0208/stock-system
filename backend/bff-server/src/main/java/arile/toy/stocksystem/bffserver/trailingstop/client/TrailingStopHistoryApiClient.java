package arile.toy.stocksystem.bffserver.trailingstop.client;

import arile.toy.stocksystem.bffserver.history.client.HistoryUriBuilder;
import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopHistoryItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrailingStopHistoryApiClient {

    private final RestClient restClient;

    @Value("${stock-api.base-url}")
    private String baseUrl;

    public HistoryPageResponse<TrailingStopHistoryItem> getHistory(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/trailing-stops/" + username + "/history", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<TrailingStopHistoryItem> getCancels(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/trailing-stops/" + username + "/cancels", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<TrailingStopHistoryItem> getUnfilled(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/trailing-stops/" + username + "/unfilled", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<TrailingStopHistoryItem> getTriggered(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/trailing-stops/" + username + "/triggered", stockCode, from, to, page, size);
    }

    private HistoryPageResponse<TrailingStopHistoryItem> get(
            String path, String stockCode, Instant from, Instant to, int page, int size) {
        try {
            URI uri = HistoryUriBuilder.build(baseUrl, path, stockCode, from, to, page, size);

            return restClient.get().uri(uri).retrieve()
                    .body(new ParameterizedTypeReference<HistoryPageResponse<TrailingStopHistoryItem>>() {});
        } catch (RestClientException e) {
            log.warn("Trailing stop history API call failed. path={}", path, e);
            return null;
        }
    }
}

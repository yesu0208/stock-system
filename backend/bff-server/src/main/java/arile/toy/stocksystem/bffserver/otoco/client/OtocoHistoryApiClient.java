package arile.toy.stocksystem.bffserver.otoco.client;

import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoHistoryItem;
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
public class OtocoHistoryApiClient {

    private final RestClient restClient;

    @Value("${stock-api.base-url}")
    private String baseUrl;

    public HistoryPageResponse<OtocoHistoryItem> getHistory(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/otocos/" + username + "/history", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<OtocoHistoryItem> getCancels(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/otocos/" + username + "/cancels", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<OtocoHistoryItem> getUnfilled(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/otocos/" + username + "/unfilled", stockCode, from, to, page, size);
    }

    public HistoryPageResponse<OtocoHistoryItem> getCompleted(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/otocos/" + username + "/completed", stockCode, from, to, page, size);
    }

    private HistoryPageResponse<OtocoHistoryItem> get(
            String path, String stockCode, Instant from, Instant to, int page, int size) {
        try {
            StringBuilder uri = new StringBuilder(baseUrl + path + "?page=" + page + "&size=" + size);
            if (stockCode != null) uri.append("&stockCode=").append(stockCode);
            if (from != null) uri.append("&from=").append(from);
            if (to != null) uri.append("&to=").append(to);

            return restClient.get().uri(uri.toString()).retrieve()
                    .body(new ParameterizedTypeReference<HistoryPageResponse<OtocoHistoryItem>>() {});
        } catch (RestClientException e) {
            log.warn("Otoco history API call failed. path={}", path, e);
            return null;
        }
    }
}

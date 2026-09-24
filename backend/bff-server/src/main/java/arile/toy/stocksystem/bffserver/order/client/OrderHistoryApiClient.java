package arile.toy.stocksystem.bffserver.order.client;

import arile.toy.stocksystem.bffserver.history.client.HistoryUriBuilder;
import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.order.dto.OrderHistoryItem;
import arile.toy.stocksystem.bffserver.trade.dto.TradeHistoryItem;
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
public class OrderHistoryApiClient {

    private final RestClient restClient;

    @Value("${stock-api.base-url}")
    private String baseUrl;

    public HistoryPageResponse<OrderHistoryItem> getHistory(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/orders/" + username + "/history", stockCode, from, to, page, size,
                new ParameterizedTypeReference<HistoryPageResponse<OrderHistoryItem>>() {});
    }

    public HistoryPageResponse<OrderHistoryItem> getCancels(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/orders/" + username + "/cancels", stockCode, from, to, page, size,
                new ParameterizedTypeReference<HistoryPageResponse<OrderHistoryItem>>() {});
    }

    public HistoryPageResponse<OrderHistoryItem> getUnfilled(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/orders/" + username + "/unfilled", stockCode, from, to, page, size,
                new ParameterizedTypeReference<HistoryPageResponse<OrderHistoryItem>>() {});
    }

    public HistoryPageResponse<TradeHistoryItem> getTrades(
            String username, String stockCode, Instant from, Instant to, int page, int size) {
        return get("/internal/orders/" + username + "/trades", stockCode, from, to, page, size,
                new ParameterizedTypeReference<HistoryPageResponse<TradeHistoryItem>>() {});
    }

    private <T> T get(String path, String stockCode, Instant from, Instant to, int page, int size,
                      ParameterizedTypeReference<T> type) {
        try {
            URI uri = HistoryUriBuilder.build(baseUrl, path, stockCode, from, to, page, size);

            return restClient.get().uri(uri).retrieve().body(type);
        } catch (RestClientException e) {
            log.warn("Order history API call failed. path={}", path, e);
            return null;
        }
    }
}

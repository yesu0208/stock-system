package arile.toy.stocksystem.bffserver.market.holiday.client;

import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCreateRequest;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketHolidayApiClient {

    private final RestClient restClient;

    @Value("${stock-api.base-url}")
    private String baseUrl;

    public List<MarketHolidayResponse> getHolidays() {
        try {
            return restClient.get()
                    .uri(baseUrl + "/internal/market/holidays")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MarketHolidayResponse>>() {});
        } catch (RestClientException e) {
            log.warn("휴장일 목록 조회 실패", e);
            return List.of();
        }
    }

    public MarketHolidayResponse addHoliday(MarketHolidayCreateRequest request) {
        return restClient.post()
                .uri(baseUrl + "/internal/market/holidays")
                .body(request)
                .retrieve()
                .body(MarketHolidayResponse.class);
    }

    public void removeHoliday(LocalDate date) {
        restClient.delete()
                .uri(baseUrl + "/internal/market/holidays/" + date)
                .retrieve()
                .toBodilessEntity();
    }
}

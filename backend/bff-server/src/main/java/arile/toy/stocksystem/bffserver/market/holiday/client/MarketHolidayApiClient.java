package arile.toy.stocksystem.bffserver.market.holiday.client;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCreateRequest;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketHolidayApiClient {

    private static final String UNAVAILABLE_MESSAGE = "휴장일 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.";

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
        try {
            return restClient.post()
                    .uri(baseUrl + "/internal/market/holidays")
                    .body(request)
                    .retrieve()
                    .body(MarketHolidayResponse.class);
        } catch (RestClientException e) {
            throw toClientError("휴장일 등록 실패. date=" + request.holidayDate(), e);
        }
    }

    public void removeHoliday(LocalDate date) {
        try {
            restClient.delete()
                    .uri(baseUrl + "/internal/market/holidays/" + date)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw toClientError("휴장일 삭제 실패. date=" + date, e);
        }
    }

    /**
     * stock-server의 4xx(중복 등록, 없는 날짜 삭제 등)는 관리자 입력 문제이므로 같은 상태로 전달하고,
     * 5xx·연결 실패는 503으로 바꿈. 둘 다 서버 버그가 아니므로 Slack 에러 알림 대상에서 제외
     */
    private ClientErrorException toClientError(String context, RestClientException e) {
        if (e instanceof RestClientResponseException response && response.getStatusCode().is4xxClientError()) {
            log.warn("{} status={}", context, response.getStatusCode());
            return new ClientErrorException(
                    HttpStatus.valueOf(response.getStatusCode().value()),
                    "휴장일 요청을 처리할 수 없습니다. 날짜를 확인해 주세요.");
        }

        log.error("{}", context, e);
        return new ClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_MESSAGE);
    }
}

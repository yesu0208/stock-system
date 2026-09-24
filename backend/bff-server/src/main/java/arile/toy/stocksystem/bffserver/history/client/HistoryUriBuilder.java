package arile.toy.stocksystem.bffserver.history.client;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 내부 서버(stock-server, account-server) 이력 조회 요청 URI를 생성.
 * 사용자 입력(stockCode 등)을 파라미터별로 인코딩해 다른 파라미터를 끼워 넣지 못하게 하고,
 * 한 번에 조회하는 개수를 제한해 과도한 조회로 인한 DB 부하를 방지.
 */
public final class HistoryUriBuilder {

    static final int MAX_PAGE_SIZE = 100;

    private HistoryUriBuilder() {
    }

    /** 주문·자동 주문·트레일링 스탑·OTOCO 이력 (종목 필터, 시각 단위 기간) */
    public static URI build(String baseUrl, String path, String stockCode, Instant from, Instant to,
                            int page, int size) {
        return create(baseUrl, path, stockCode, from, to, page, size);
    }

    /** 일일 수익률 이력 (날짜 단위 기간, 종목 필터 없음) */
    public static URI build(String baseUrl, String path, LocalDate from, LocalDate to, int page, int size) {
        return create(baseUrl, path, null, from, to, page, size);
    }

    private static URI create(String baseUrl, String path, String stockCode, Object from, Object to,
                              int page, int size) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .queryParam("page", Math.max(page, 0))
                .queryParam("size", Math.clamp(size, 1, MAX_PAGE_SIZE))
                .queryParamIfPresent("stockCode", Optional.ofNullable(stockCode))
                .queryParamIfPresent("from", Optional.ofNullable(from))
                .queryParamIfPresent("to", Optional.ofNullable(to))
                .encode()
                .build()
                .toUri();
    }
}

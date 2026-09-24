package arile.toy.stocksystem.bffserver.history.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryUriBuilderTest {

    private static final String BASE_URL = "http://stock-server:8082";
    private static final String PATH = "/internal/orders/user1/history";

    private static String queryParam(URI uri, String name) {
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst(name);
    }

    @Test
    @DisplayName("기본 경로에 페이지·개수·종목·기간을 쿼리 파라미터로 붙인다")
    void build() {
        URI uri = HistoryUriBuilder.build(BASE_URL, PATH, "005930",
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-24T00:00:00Z"), 2, 50);

        assertThat(uri.getHost()).isEqualTo("stock-server");
        assertThat(uri.getPort()).isEqualTo(8082);
        assertThat(uri.getPath()).isEqualTo(PATH);
        assertThat(queryParam(uri, "page")).isEqualTo("2");
        assertThat(queryParam(uri, "size")).isEqualTo("50");
        assertThat(queryParam(uri, "stockCode")).isEqualTo("005930");
        assertThat(queryParam(uri, "from")).isEqualTo("2026-09-01T00:00:00Z");
        assertThat(queryParam(uri, "to")).isEqualTo("2026-09-24T00:00:00Z");
    }

    @Test
    @DisplayName("종목·기간이 없으면 해당 파라미터를 붙이지 않는다")
    void optionalParams() {
        URI uri = HistoryUriBuilder.build(BASE_URL, PATH, null, null, null, 0, 20);

        assertThat(uri.getQuery()).isEqualTo("page=0&size=20");
    }

    @Test
    @DisplayName("종목코드에 &·=·#가 있어도 값으로만 전달되어 다른 파라미터를 끼워 넣지 못한다")
    void specialCharactersEncoded() {
        URI uri = HistoryUriBuilder.build(BASE_URL, PATH, "005930&size=1000000#x", null, null, 0, 20);

        assertThat(uri.getRawQuery()).doesNotContain("&size=1000000").doesNotContain("#");
        assertThat(UriComponentsBuilder.fromUri(uri).build().getQueryParams().get("size")).containsExactly("20");
        assertThat(queryParam(uri, "stockCode")).isEqualTo("005930%26size%3D1000000%23x");
    }

    @ParameterizedTest(name = "size={0} → {1}")
    @CsvSource({"0, 1", "-5, 1", "1, 1", "100, 100", "101, 100", "1000000, 100"})
    @DisplayName("조회 개수는 1~100으로 제한한다 (과도한 조회로 인한 DB 부하 방지)")
    void sizeClamped(int size, int expected) {
        URI uri = HistoryUriBuilder.build(BASE_URL, PATH, null, null, null, 0, size);

        assertThat(queryParam(uri, "size")).isEqualTo(String.valueOf(expected));
    }

    @ParameterizedTest(name = "page={0} → {1}")
    @CsvSource({"-1, 0", "0, 0", "3, 3"})
    @DisplayName("페이지가 음수면 0으로 바꾼다")
    void pageNotNegative(int page, int expected) {
        URI uri = HistoryUriBuilder.build(BASE_URL, PATH, null, null, null, page, 20);

        assertThat(queryParam(uri, "page")).isEqualTo(String.valueOf(expected));
    }

    @Test
    @DisplayName("날짜 단위 기간은 yyyy-MM-dd 형식으로 붙이고, 종목 파라미터는 없으며 개수 제한도 같다")
    void build_localDate() {
        URI uri = HistoryUriBuilder.build("http://account-server", "/internal/returns/user1/history",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 24), -1, 1_000_000);

        assertThat(uri.getPath()).isEqualTo("/internal/returns/user1/history");
        assertThat(queryParam(uri, "from")).isEqualTo("2026-09-01");
        assertThat(queryParam(uri, "to")).isEqualTo("2026-09-24");
        assertThat(queryParam(uri, "page")).isEqualTo("0");
        assertThat(queryParam(uri, "size")).isEqualTo("100");
        assertThat(queryParam(uri, "stockCode")).isNull();
    }

    @Test
    @DisplayName("기간·종목 없는 이력(랭크)도 같은 개수·페이지 제한을 적용한다")
    void build_pagingOnly() {
        URI uri = HistoryUriBuilder.build("http://account-server", "/internal/ranks/user1/history", -1, 1_000_000);

        assertThat(uri.getPath()).isEqualTo("/internal/ranks/user1/history");
        assertThat(uri.getQuery()).isEqualTo("page=0&size=100");
    }
}

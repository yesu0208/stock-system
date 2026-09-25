package arile.toy.stocksystem.bffserver.market.holiday.client;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import arile.toy.stocksystem.bffserver.market.holiday.dto.MarketHolidayCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MarketHolidayApiClientTest {

    private static final String BASE_URL = "http://stock-server";
    private static final String HOLIDAYS = BASE_URL + "/internal/market/holidays";
    private static final LocalDate DATE = LocalDate.of(2026, 10, 9);

    private MockRestServiceServer server;
    private MarketHolidayApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MarketHolidayApiClient(builder.build());
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    private static void assertStatus(Runnable call, HttpStatus expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ClientErrorException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(expected));
    }

    @Test
    @DisplayName("휴장일 목록을 조회하고, 실패하면 빈 목록을 반환한다")
    void getHolidays() {
        server.expect(requestTo(HOLIDAYS)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(HOLIDAYS)).andRespond(withServerError());

        assertThat(client.getHolidays()).isEmpty();
        assertThat(client.getHolidays()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("휴장일 등록: 날짜·메모를 담아 POST로 요청한다")
    void addHoliday() {
        server.expect(requestTo(HOLIDAYS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.holidayDate").value("2026-10-09"))
                .andExpect(jsonPath("$.memo").value("한글날"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.addHoliday(new MarketHolidayCreateRequest(DATE, "한글날"));

        server.verify();
    }

    @Test
    @DisplayName("휴장일 등록: stock-server의 4xx(중복 등록 등)는 같은 상태로 전달한다 (500·Slack 아님)")
    void addHoliday_clientError_passedThrough() {
        server.expect(requestTo(HOLIDAYS)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertStatus(() -> client.addHoliday(new MarketHolidayCreateRequest(DATE, "한글날")), HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("휴장일 등록: stock-server 장애(5xx)는 503으로 바꾼다")
    void addHoliday_serverError_503() {
        server.expect(requestTo(HOLIDAYS)).andRespond(withServerError());

        assertStatus(() -> client.addHoliday(new MarketHolidayCreateRequest(DATE, "한글날")),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("휴장일 삭제: 날짜를 경로에 담아 DELETE로 요청한다")
    void removeHoliday() {
        server.expect(requestTo(HOLIDAYS + "/2026-10-09"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        assertThatCode(() -> client.removeHoliday(DATE)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("휴장일 삭제: 없는 날짜(404)는 같은 상태로, 장애는 503으로 전달한다")
    void removeHoliday_errors() {
        server.expect(requestTo(HOLIDAYS + "/2026-10-09")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(HOLIDAYS + "/2026-10-09")).andRespond(withServerError());

        assertStatus(() -> client.removeHoliday(DATE), HttpStatus.NOT_FOUND);
        assertStatus(() -> client.removeHoliday(DATE), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("휴장일 등록: stock-server에 연결할 수 없으면(응답 없음) 503으로 바꾼다")
    void addHoliday_connectionFailure_503() {
        server.expect(requestTo(HOLIDAYS)).andRespond(withException(new IOException("Connection refused")));

        assertStatus(() -> client.addHoliday(new MarketHolidayCreateRequest(DATE, "한글날")),
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}

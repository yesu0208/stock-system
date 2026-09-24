package arile.toy.stocksystem.bffserver.trailingstop.client;

import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopHistoryItem;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TrailingStopHistoryApiClientTest {

    private static final String BASE_URL = "http://stock-server";
    private static final String PREFIX = BASE_URL + "/internal/trailing-stops/user1/";

    private static final TrailingStopStatus STATUS = TrailingStopStatus.values()[0];
    private static final String PAGE_JSON = """
            {"items": [{"trailingStopId": 5, "stockCode": "005930", "trailingStopType": "SELL", "leverageRatio": "X2",
                        "orderQuantity": 10, "stopPercent": 3.0, "basePrice": 72000, "triggerPrice": 69840,
                        "trailingStopStatus": "%s", "orderTime": "2026-09-24T00:30:00Z"}],
             "page": 0, "size": 20, "totalElements": 1, "hasNext": false}
            """.formatted(STATUS.name());

    private MockRestServiceServer server;
    private TrailingStopHistoryApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TrailingStopHistoryApiClient(builder.build());
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("stock-server 이력 항목(추적 폭·기준가·발동가 포함)을 필드 그대로 역직렬화한다")
    void getHistory_deserializesItem() {
        server.expect(requestTo(PREFIX + "history?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        HistoryPageResponse<TrailingStopHistoryItem> response = client.getHistory("user1", null, null, null, 0, 20);

        assertThat(response.items()).containsExactly(new TrailingStopHistoryItem(
                5L, "005930", TrailingStopType.SELL, LeverageRatio.X2, 10, 3.0, 72_000, 69_840,
                STATUS, Instant.parse("2026-09-24T00:30:00Z")));
        server.verify();
    }

    @Test
    @DisplayName("취소·미체결·발동 이력은 각자의 경로로 요청한다")
    void otherHistories() {
        server.expect(requestTo(PREFIX + "cancels?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PREFIX + "unfilled?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PREFIX + "triggered?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.getCancels("user1", null, null, null, 0, 20)).isNotNull();
        assertThat(client.getUnfilled("user1", null, null, null, 0, 20)).isNotNull();
        assertThat(client.getTriggered("user1", null, null, null, 0, 20)).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("stock-server 호출이 실패하면 null을 반환한다")
    void serverError_returnsNull() {
        server.expect(requestTo(PREFIX + "history?page=0&size=20")).andRespond(withServerError());

        assertThat(client.getHistory("user1", null, null, null, 0, 20)).isNull();
    }
}

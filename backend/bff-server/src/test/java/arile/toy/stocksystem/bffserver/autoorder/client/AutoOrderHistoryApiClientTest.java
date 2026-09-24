package arile.toy.stocksystem.bffserver.autoorder.client;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderHistoryItem;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
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

class AutoOrderHistoryApiClientTest {

    private static final String BASE_URL = "http://stock-server";
    private static final String PREFIX = BASE_URL + "/internal/auto-orders/user1/";

    /** stock-server AutoOrderHistoryItem.fromEntity가 만드는 레버리지 자동 주문 한 건 */
    private static final String PAGE_JSON = """
            {"items": [{"autoOrderId": 7, "stockCode": "005930", "autoOrderType": "BUY", "leverageRatio": "X2",
                        "triggerPrice": 68000, "orderPrice": 69000, "orderQuantity": 10,
                        "autoOrderStatus": "TRIGGERED", "orderTime": "2026-09-24T00:30:00Z",
                        "notionalValue": 690000, "initialMargin": 345000,
                        "maintenanceMarginRate": 1.4, "liquidationPrice": 48300}],
             "page": 0, "size": 20, "totalElements": 1, "hasNext": false}
            """;

    private MockRestServiceServer server;
    private AutoOrderHistoryApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AutoOrderHistoryApiClient(builder.build());
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("stock-server 이력 항목을 필드 그대로 역직렬화한다 (두 서버의 이력 항목 필드가 일치해야 함)")
    void getHistory_deserializesItem() {
        server.expect(requestTo(PREFIX + "history?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        HistoryPageResponse<AutoOrderHistoryItem> response = client.getHistory("user1", null, null, null, 0, 20);

        assertThat(response.items()).containsExactly(new AutoOrderHistoryItem(
                7L, "005930", AutoOrderType.BUY, LeverageRatio.X2, 68_000, 69_000, 10,
                AutoOrderStatus.TRIGGERED, Instant.parse("2026-09-24T00:30:00Z"),
                690_000L, 345_000L, 1.4, 48_300L));
        assertThat(response.totalElements()).isEqualTo(1L);
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

    @Test
    @DisplayName("자동 주문 상태는 대기·발동·취소 세 가지다 (stock-server 상태 이름과 일치해야 함)")
    void statuses() {
        assertThat(AutoOrderStatus.values())
                .containsExactly(AutoOrderStatus.ACTIVE, AutoOrderStatus.TRIGGERED, AutoOrderStatus.CANCELED);
    }
}

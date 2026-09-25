package arile.toy.stocksystem.bffserver.otoco.client;

import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoHistoryItem;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoLeg;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoStatus;
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

class OtocoHistoryApiClientTest {

    private static final String BASE_URL = "http://stock-server";
    private static final String PREFIX = BASE_URL + "/internal/otocos/user1/";

    /** 익절로 완료된 OTOCO 한 건 (상태·체결 구간 이름은 enum 값에서 가져와 두 서버의 이름이 어긋나면 역직렬화가 실패함) */
    private static final OtocoStatus STATUS = OtocoStatus.values()[0];
    private static final OtocoLeg LEG = OtocoLeg.values()[0];
    private static final String PAGE_JSON = """
            {"items": [{"otocoId": 3, "stockCode": "005930", "entryDirection": "ABOVE", "leverageRatio": "SPOT",
                        "orderQuantity": 10, "entryTriggerPrice": 70000, "tpTriggerPrice": 75000, "slTriggerPrice": 65000,
                        "otocoStatus": "%s", "completedLeg": "%s", "orderTime": "2026-09-24T00:30:00Z"}],
             "page": 0, "size": 20, "totalElements": 1, "hasNext": false}
            """.formatted(STATUS.name(), LEG.name());

    private MockRestServiceServer server;
    private OtocoHistoryApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OtocoHistoryApiClient(builder.build());
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("stock-server 이력 항목(익절·손절가, 완료 구간 포함)을 필드 그대로 역직렬화한다")
    void getHistory_deserializesItem() {
        server.expect(requestTo(PREFIX + "history?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        HistoryPageResponse<OtocoHistoryItem> response = client.getHistory("user1", null, null, null, 0, 20);

        assertThat(response.items()).containsExactly(new OtocoHistoryItem(
                3L, "005930", OtocoEntryDirection.ABOVE, LeverageRatio.SPOT, 10, 70_000, 75_000, 65_000,
                STATUS, LEG, Instant.parse("2026-09-24T00:30:00Z")));
        server.verify();
    }

    @Test
    @DisplayName("취소·미체결·완료 이력은 각자의 경로로 요청한다")
    void otherHistories() {
        server.expect(requestTo(PREFIX + "cancels?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PREFIX + "unfilled?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PREFIX + "completed?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.getCancels("user1", null, null, null, 0, 20)).isNotNull();
        assertThat(client.getUnfilled("user1", null, null, null, 0, 20)).isNotNull();
        assertThat(client.getCompleted("user1", null, null, null, 0, 20)).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("stock-server 호출이 실패하면 null을 반환한다")
    void serverError_returnsNull() {
        server.expect(requestTo(PREFIX + "history?page=0&size=20")).andRespond(withServerError());

        assertThat(client.getHistory("user1", null, null, null, 0, 20)).isNull();
    }
}

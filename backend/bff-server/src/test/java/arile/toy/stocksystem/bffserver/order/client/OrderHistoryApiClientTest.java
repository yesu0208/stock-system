package arile.toy.stocksystem.bffserver.order.client;

import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.order.dto.OrderHistoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OrderHistoryApiClientTest {

    private static final String BASE_URL = "http://stock-server";

    /** stock-server HistoryPageResponse.of(page)가 보내는 모양 (기본형 필드는 모두 있어야 역직렬화됨) */
    private static final String PAGE_JSON = """
            {"items": [], "page": 1, "size": 20, "totalElements": 25, "hasNext": false}
            """;

    private MockRestServiceServer server;
    private OrderHistoryApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrderHistoryApiClient(builder.build());
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("사용자별 주문 이력 경로로 페이지·개수를 붙여 요청하고 응답을 변환한다")
    void getHistory() {
        server.expect(requestTo(BASE_URL + "/internal/orders/user1/history?page=1&size=20"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        HistoryPageResponse<OrderHistoryItem> response = client.getHistory("user1", null, null, null, 1, 20);

        assertThat(response).isNotNull();
        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(25L);
        assertThat(response.hasNext()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("취소·미체결·체결 이력은 각자의 경로로 요청한다")
    void otherHistories() {
        server.expect(requestTo(BASE_URL + "/internal/orders/user1/cancels?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/internal/orders/user1/unfilled?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/internal/orders/user1/trades?page=0&size=20"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.getCancels("user1", null, null, null, 0, 20)).isNotNull();
        assertThat(client.getUnfilled("user1", null, null, null, 0, 20)).isNotNull();
        assertThat(client.getTrades("user1", null, null, null, 0, 20)).isNotNull();

        server.verify();
    }

    @Test
    @DisplayName("종목코드에 섞인 파라미터를 끼워 넣지 못하고, 조회 개수는 100으로 제한된 채 요청된다")
    void injectionBlocked() {
        server.expect(requestTo(startsWith(BASE_URL + "/internal/orders/user1/history")))
                .andExpect(queryParam("size", "100"))
                .andExpect(queryParam("stockCode", "005930%26size%3D1"))
                .andRespond(withSuccess(PAGE_JSON, MediaType.APPLICATION_JSON));

        client.getHistory("user1", "005930&size=1", null, null, 0, 1_000_000);

        server.verify();
    }

    @Test
    @DisplayName("stock-server 호출이 실패하면 null을 반환한다")
    void serverError_returnsNull() {
        server.expect(requestTo(BASE_URL + "/internal/orders/user1/history?page=0&size=20"))
                .andRespond(withServerError());

        assertThat(client.getHistory("user1", null, null, null, 0, 20)).isNull();
    }
}

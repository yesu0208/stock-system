package arile.toy.stocksystem.bffserver.dailyreturn.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DailyReturnApiClientTest {

    private static final String BASE_URL = "http://account-server";

    /** account-server DailyReturnHistoryResponse 모양 (기본형 필드 hasNext는 있어야 역직렬화됨) */
    private static final String RESPONSE_JSON = """
            {"items": [], "hasNext": false}
            """;

    private MockRestServiceServer server;
    private DailyReturnApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DailyReturnApiClient(builder.build());
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("사용자별 수익률 이력 경로로 기간·페이지·개수를 붙여 요청한다")
    void getHistory() {
        server.expect(requestTo(BASE_URL
                        + "/internal/returns/user1/history?page=1&size=20&from=2026-09-01&to=2026-09-24"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(RESPONSE_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.getHistory("user1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 24), 1, 20))
                .isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("조회 개수는 100으로 제한하고 음수 페이지는 0으로 보정해 요청한다")
    void sizeAndPageBounded() {
        server.expect(requestTo(BASE_URL + "/internal/returns/user1/history?page=0&size=100"))
                .andRespond(withSuccess(RESPONSE_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.getHistory("user1", null, null, -1, 1_000_000)).isNotNull();

        server.verify();
    }

    @Test
    @DisplayName("account-server 호출이 실패하면 null을 반환한다")
    void serverError_returnsNull() {
        server.expect(requestTo(BASE_URL + "/internal/returns/user1/history?page=0&size=20"))
                .andRespond(withServerError());

        assertThat(client.getHistory("user1", null, null, 0, 20)).isNull();
    }
}

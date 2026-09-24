package arile.toy.stocksystem.bffserver.rank.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RankApiClientTest {

    private static final String BASE_URL = "http://account-server";

    private MockRestServiceServer server;
    private RankApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RankApiClient(builder.build());
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("사용자 랭크를 조회한다")
    void getRank() {
        server.expect(requestTo(BASE_URL + "/internal/ranks/user1"))
                .andRespond(withSuccess("""
                        {"username": "user1", "tier": "GOLD", "division": 3, "rp": 2000,
                         "displayName": "GOLD", "currentTierMinRp": 1900, "nextTierMinRp": 2200}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.getRank("user1")).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("사용자 이름은 경로에 인코딩되어 들어간다 (경로 조작 방지)")
    void getRank_encodesUsername() {
        server.expect(requestTo(BASE_URL + "/internal/ranks/a%2Fb"))
                .andRespond(withServerError());

        client.getRank("a/b");

        server.verify();
    }

    @Test
    @DisplayName("랭크 조회가 실패하면 null을 반환한다 (랭크 없이 사용자 정보는 보여줌)")
    void getRank_fails_null() {
        server.expect(requestTo(BASE_URL + "/internal/ranks/user1")).andRespond(withServerError());

        assertThat(client.getRank("user1")).isNull();
    }

    @Test
    @DisplayName("랭크 이력은 페이지·개수를 붙여 요청하고, 개수는 100으로 제한한다")
    void getRankHistory() {
        server.expect(requestTo(BASE_URL + "/internal/ranks/user1/history?page=0&size=20"))
                .andRespond(withSuccess("{\"items\": [], \"hasNext\": false}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/internal/ranks/user1/history?page=2&size=100"))
                .andRespond(withSuccess("{\"items\": [], \"hasNext\": true}", MediaType.APPLICATION_JSON));

        assertThat(client.getRankHistory("user1", 0, 20)).isNotNull();
        assertThat(client.getRankHistory("user1", 2, 1_000_000)).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("랭크 이력 조회가 실패하면 null을 반환한다")
    void getRankHistory_fails_null() {
        server.expect(requestTo(BASE_URL + "/internal/ranks/user1/history?page=0&size=20"))
                .andRespond(withServerError());

        assertThat(client.getRankHistory("user1", 0, 20)).isNull();
    }
}

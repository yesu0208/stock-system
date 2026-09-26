package arile.toy.stocksystem.stockserver.useraccount.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("[Client] 계좌 서버 API 클라이언트 테스트")
class AccountApiClientTest {

    private static final String BASE_URL = "http://account";

    private MockRestServiceServer server;
    private AccountApiClient sut;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sut = new AccountApiClient(builder.build());
        ReflectionTestUtils.setField(sut, "baseUrl", BASE_URL);
    }

    @DisplayName("예약·환불 요청은 경로와 본문을 보내고 응답의 success를 돌려준다")
    @Test
    void whenReservingAndRefunding_thenPostsAndReturnsSuccess() {
        expectPost("/internal/accounts/user/reserve-cash", "{\"amount\":700105}", true);
        expectPost("/internal/accounts/user/refund-cash", "{\"amount\":100}", false);
        expectPost("/internal/accounts/user/reserve-stock", "{\"stockCode\":\"005930\",\"quantity\":10}", true);
        expectPost("/internal/accounts/user/refund-stock", "{\"stockCode\":\"005930\",\"quantity\":10}", true);
        expectPost("/internal/accounts/user/reserve-leverage-stock",
                "{\"stockCode\":\"005930\",\"leverageRatio\":\"X2\",\"quantity\":10}", true);
        expectPost("/internal/accounts/user/refund-leverage-stock",
                "{\"stockCode\":\"005930\",\"leverageRatio\":\"X2\",\"quantity\":10}", true);

        assertThat(sut.reserveCash("user", 700_105L)).isTrue();
        assertThat(sut.refundReservedCash("user", 100L)).isFalse();
        assertThat(sut.reserveStock("user", "005930", 10)).isTrue();
        assertThat(sut.refundReservedStock("user", "005930", 10)).isTrue();
        assertThat(sut.reserveLeverageStock("user", "005930", "X2", 10)).isTrue();
        assertThat(sut.refundReservedLeverageStock("user", "005930", "X2", 10)).isTrue();
        server.verify();
    }

    @DisplayName("서버 오류면 예외 대신 false를 돌려준다")
    @Test
    void givenServerError_whenReserving_thenFalse() {
        server.expect(requestTo(BASE_URL + "/internal/accounts/user/reserve-cash")).andRespond(withServerError());

        assertThat(sut.reserveCash("user", 1L)).isFalse();
    }

    @DisplayName("정산 요청을 보내고, 실패해도 예외를 던지지 않는다")
    @Test
    void whenSettling_thenPostsAndSwallowsErrors() {
        server.expect(requestTo(BASE_URL + "/internal/accounts/settle"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"usernames\":[\"user\"]}"))
                .andRespond(withSuccess());
        server.expect(requestTo(BASE_URL + "/internal/accounts/settle-all"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());
        server.expect(requestTo(BASE_URL + "/internal/accounts/settle-all")).andRespond(withServerError());

        sut.settle(Set.of("user"));
        sut.settleAll();
        assertThatNoException().isThrownBy(() -> sut.settleAll());
        server.verify();
    }

    @DisplayName("정산 요청이 서버 오류로 실패해도 예외를 던지지 않는다")
    @Test
    void givenServerError_whenSettling_thenNoException() {
        server.expect(requestTo(BASE_URL + "/internal/accounts/settle"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatNoException().isThrownBy(() -> sut.settle(Set.of("user")));
        server.verify();
    }

    @DisplayName("응답 본문이 없으면 false를 돌려준다")
    @Test
    void givenNullBody_whenReserving_thenFalse() {
        server.expect(requestTo(BASE_URL + "/internal/accounts/user/reserve-cash"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThat(sut.reserveCash("user", 100L)).isFalse();
        server.verify();
    }

    private void expectPost(String path, String body, boolean success) {
        server.expect(requestTo(BASE_URL + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(body))
                .andRespond(withSuccess("{\"success\":" + success + "}", MediaType.APPLICATION_JSON));
    }
}

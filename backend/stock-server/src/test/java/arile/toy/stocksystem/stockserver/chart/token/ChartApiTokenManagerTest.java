package arile.toy.stocksystem.stockserver.chart.token;

import arile.toy.stocksystem.stockserver.chart.StubWebClients;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("[Token] 차트 API 접근 토큰 관리 테스트")
class ChartApiTokenManagerTest {

    @DisplayName("발급받은 토큰을 보관하고, 응답에 토큰이 없으면 기존 값을 유지한다")
    @Test
    void whenRefreshing_thenStoresTokenOrKeeps() {
        List<ClientRequest> requests = new ArrayList<>();
        List<String> responses = new ArrayList<>(List.of("{\"access_token\":\"t1\"}", "{}"));
        var sut = new ChartApiTokenManager(StubWebClients.of(requests, r -> responses.remove(0)));
        ReflectionTestUtils.setField(sut, "appKey", "key");
        ReflectionTestUtils.setField(sut, "appSecret", "secret");

        sut.init();
        assertThat(sut.getAccessToken()).isEqualTo("t1");
        assertThat(requests.get(0).url().getPath()).isEqualTo("/oauth2/tokenP");

        sut.scheduledRefresh();
        assertThat(sut.getAccessToken()).isEqualTo("t1");
    }

    @DisplayName("발급 중 오류가 나도 예외를 던지지 않는다 (기동 실패 방지)")
    @Test
    void givenError_whenRefreshing_thenDoesNotThrow() {
        var sut = new ChartApiTokenManager(StubWebClients.of(r -> { throw new IllegalStateException("down"); }));

        assertThatNoException().isThrownBy(sut::init);
        assertThat(sut.getAccessToken()).isNull();
    }

    @DisplayName("응답 본문이 없으면 토큰을 갱신하지 않는다")
    @Test
    void givenNullBody_whenRefreshing_thenKeepsToken() {
        var sut = new ChartApiTokenManager(StubWebClients.of(r -> "null"));

        sut.init();

        assertThat(sut.getAccessToken()).isNull();
    }

    @DisplayName("오류 응답(4xx/5xx)이면 예외 없이 토큰을 유지한다")
    @Test
    void givenErrorStatus_whenRefreshing_thenKeepsToken() {
        var sut = new ChartApiTokenManager(statusClient(HttpStatus.INTERNAL_SERVER_ERROR, "{\"error\":\"down\"}"));

        assertThatNoException().isThrownBy(sut::init);
        assertThat(sut.getAccessToken()).isNull();
    }

    @DisplayName("요청 중 예외가 나면 예외 없이 토큰을 유지한다")
    @Test
    void givenExchangeError_whenRefreshing_thenKeepsToken() {
        WebClient client = WebClient.builder()
                .baseUrl("http://chart")
                .exchangeFunction(request -> Mono.error(new IllegalStateException("connection refused")))
                .build();
        var sut = new ChartApiTokenManager(client);

        assertThatNoException().isThrownBy(sut::init);
        assertThat(sut.getAccessToken()).isNull();
    }

    private static WebClient statusClient(HttpStatus status, String body) {
        return WebClient.builder()
                .baseUrl("http://chart")
                .exchangeFunction(request -> Mono.just(ClientResponse.create(status)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build()))
                .build();
    }
}

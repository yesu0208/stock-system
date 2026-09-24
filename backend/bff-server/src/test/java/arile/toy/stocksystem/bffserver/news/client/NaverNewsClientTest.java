package arile.toy.stocksystem.bffserver.news.client;

import arile.toy.stocksystem.bffserver.exception.ClientErrorException;
import arile.toy.stocksystem.bffserver.news.dto.NaverNewsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaverNewsClientTest {

    private static NaverNewsClient client(ExchangeFunction exchange) {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://openapi.naver.com")
                .exchangeFunction(exchange)
                .build();
        NaverNewsClient client = new NaverNewsClient(webClient);
        ReflectionTestUtils.setField(client, "clientId", "test-id");
        ReflectionTestUtils.setField(client, "clientSecret", "test-secret");
        return client;
    }

    private static void assertUnavailable(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ClientErrorException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getMessage()).isEqualTo("뉴스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
                });
    }

    @Test
    @DisplayName("최신순 100건을 인증 헤더와 함께 요청하고 응답을 변환한다")
    void search() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        NaverNewsClient client = client(request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {"lastBuildDate": "x", "total": 1, "start": 1, "display": 1,
                             "items": [{"title": "삼성전자", "originallink": "o", "link": "l",
                                        "description": "d", "pubDate": "Thu, 24 Sep 2026 09:00:00 +0900"}]}
                            """)
                    .build());
        });

        NaverNewsResponse response = client.search("삼성전자");

        assertThat(response.items()).hasSize(1);
        URI uri = captured.get().url();
        assertThat(uri.getPath()).isEqualTo("/v1/search/news.json");
        assertThat(uri.getQuery()).contains("display=100", "start=1", "sort=date");
        assertThat(captured.get().headers().getFirst("X-Naver-Client-Id")).isEqualTo("test-id");
        assertThat(captured.get().headers().getFirst("X-Naver-Client-Secret")).isEqualTo("test-secret");
    }

    @Test
    @DisplayName("네이버가 오류 응답(인증 실패·한도 초과 등)을 주면 503 ClientErrorException으로 바꾼다")
    void errorResponse_503() {
        NaverNewsClient client = client(request -> Mono.just(
                ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS).body("{\"errorCode\":\"012\"}").build()));

        assertUnavailable(() -> client.search("삼성전자"));
    }

    @Test
    @DisplayName("연결 실패·타임아웃이면 503 ClientErrorException으로 바꾼다")
    void connectionFailure_503() {
        NaverNewsClient client = client(request -> Mono.error(new WebClientRequestException(
                new IOException("timeout"), HttpMethod.GET, request.url(), HttpHeaders.EMPTY)));

        assertUnavailable(() -> client.search("삼성전자"));
    }
}

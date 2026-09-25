package arile.toy.stocksystem.stockserver.chart.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Config] 차트 API WebClient 설정 테스트")
class StockChartWebClientConfigTest {

    @Test
    @DisplayName("설정된 base-url을 기준으로 상대 경로 요청을 보낸다")
    void chartApiWebClient() {
        WebClient webClient = new StockChartWebClientConfig().chartApiWebClient("https://chart.example.com");
        AtomicReference<URI> requested = new AtomicReference<>();

        // 실제 호출 대신 요청 URL만 기록하도록 exchange 함수를 바꿔서 확인
        webClient.mutate()
                .exchangeFunction((ClientRequest request) -> {
                    requested.set(request.url());
                    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                })
                .build()
                .get().uri("/oauth2/tokenP")
                .retrieve()
                .toBodilessEntity()
                .block();

        assertThat(requested.get()).isEqualTo(URI.create("https://chart.example.com/oauth2/tokenP"));
    }
}

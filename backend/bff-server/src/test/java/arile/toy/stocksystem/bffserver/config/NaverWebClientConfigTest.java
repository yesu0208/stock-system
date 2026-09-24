package arile.toy.stocksystem.bffserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class NaverWebClientConfigTest {

    @Test
    @DisplayName("네이버 오픈 API를 base URL로 하는 WebClient를 생성한다")
    void naverWebClient() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();

        WebClient client = new NaverWebClientConfig().naverWebClient().mutate()
                .exchangeFunction(request -> {
                    requestedUri.set(request.url());
                    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                })
                .build();

        client.get().uri("/v1/search/news.json").retrieve().toBodilessEntity().block();

        assertThat(requestedUri.get()).hasToString("https://openapi.naver.com/v1/search/news.json");
    }
}

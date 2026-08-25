package arile.toy.stocksystem.bffserver.news.client;

import arile.toy.stocksystem.bffserver.news.dto.NaverNewsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class NaverNewsClient {

    private final WebClient naverWebClient;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    public NaverNewsClient(@Qualifier("naverWebClient") WebClient naverWebClient) {
        this.naverWebClient = naverWebClient;
    }

    public NaverNewsResponse search(String keyword) {
        try {
            return naverWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/news.json")
                            .queryParam("query", keyword)
                            .queryParam("display", 100)
                            .queryParam("start", 1)
                            .queryParam("sort", "date")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .bodyToMono(NaverNewsResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Naver news API error. status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("네이버 뉴스 API 요청 실패", e);
        }
    }
}
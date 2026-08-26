package arile.toy.stocksystem.bffserver.chart.token;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChartApiTokenManager {

    private final WebClient chartApiWebClient;

    @Value("${chart-api.appkey}")
    private String appKey;

    @Value("${chart-api.appsecret}")
    private String appSecret;

    private volatile String accessToken;

    @PostConstruct
    public void init() {
        refreshAccessToken();
    }

    // 명세상 유효기간 24h, 갱신주기 6h 이지만, 여유있게 12h마다 갱신
    @Scheduled(fixedRate = 12 * 60 * 60 * 1000)
    public void scheduledRefresh() {
        refreshAccessToken();
    }

    public String getAccessToken() {
        return accessToken;
    }

    private synchronized void refreshAccessToken() {
        log.info("ChartApi accessToken 발급 시도. time={}", java.time.Instant.now());
        try {
            ChartApiTokenResponse response = chartApiWebClient.post()
                    .uri("/oauth2/tokenP")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                    .bodyValue(ChartApiTokenRequest.of(appKey, appSecret))
                    .retrieve()
                    .bodyToMono(ChartApiTokenResponse.class)
                    .block();

            if (response == null || response.access_token() == null) {
                log.error("ChartApi accessToken 발급 실패: 응답이 비어있음");
                return;
            }

            this.accessToken = response.access_token();
            log.info("ChartApi accessToken 갱신 완료. expiredAt={}", response.access_token_token_expired());

        } catch (WebClientResponseException e) {
            log.error("ChartApi accessToken 발급 실패. status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("ChartApi accessToken 발급 중 오류 발생", e);
        }
    }
}
package arile.toy.stocksystem.bffserver.rank.client;

import arile.toy.stocksystem.bffserver.rank.dto.RankHistoryResponse;
import arile.toy.stocksystem.bffserver.rank.dto.RankResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankApiClient {

    private final RestClient restClient;

    @Value("${account-api.base-url}")
    private String baseUrl;

    public RankResponse getRank(String username) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/internal/ranks/" + username)
                    .retrieve()
                    .body(RankResponse.class);
        } catch (RestClientException e) {
            log.warn("Rank API call failed. username={}", username, e);
            return null;
        }
    }

    public RankHistoryResponse getRankHistory(String username, int page, int size) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/internal/ranks/" + username + "/history?page=" + page + "&size=" + size)
                    .retrieve()
                    .body(RankHistoryResponse.class);
        } catch (RestClientException e) {
            log.warn("Rank history API call failed. username={}", username, e);
            return null;
        }
    }
}

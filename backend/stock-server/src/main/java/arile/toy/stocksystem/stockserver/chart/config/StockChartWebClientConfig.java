package arile.toy.stocksystem.stockserver.chart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class StockChartWebClientConfig {

    @Bean
    public WebClient chartApiWebClient(
            @Value("${chart-api.base-url}") String baseUrl
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}

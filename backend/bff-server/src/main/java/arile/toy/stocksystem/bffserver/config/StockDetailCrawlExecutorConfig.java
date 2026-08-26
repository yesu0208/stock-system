package arile.toy.stocksystem.bffserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class StockDetailCrawlExecutorConfig {

    @Bean
    public ExecutorService stockDetailCrawlExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
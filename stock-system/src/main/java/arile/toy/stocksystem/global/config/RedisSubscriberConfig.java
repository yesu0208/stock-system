package arile.toy.stocksystem.global.config;

import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisStockSummaryEventSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisStockSummaryEventSubscriber redisStockSummaryEventSubscriber;

    @Bean
    public RedisMessageListenerContainer redisContainer() {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(redisConnectionFactory);

        container.addMessageListener(
                redisStockSummaryEventSubscriber,
                new ChannelTopic("summary:event")
        );

        return container;
    }
}

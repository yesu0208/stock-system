package arile.toy.stocksystem.bffserver.config;

import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisStockSummaryEventSubscriber;
import arile.toy.stocksystem.bffserver.market.phase.*;
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
    private final RedisMarketCloseEventSubscriber redisMarketCloseEventSubscriber;
    private final RedisMarketPhaseEventSubscriber redisMarketPhaseEventSubscriber;
    private final RedisGlobalMarketPhaseEventSubscriber redisGlobalMarketPhaseEventSubscriber;
    private final RedisInterestAppliedEventSubscriber redisInterestAppliedEventSubscriber;
    private final RedisRankUpdatedEventSubscriber redisRankUpdatedEventSubscriber;

    @Bean
    public RedisMessageListenerContainer redisContainer() {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(redisConnectionFactory);

        container.addMessageListener(
                redisStockSummaryEventSubscriber,
                new ChannelTopic("summary:event")
        );

        container.addMessageListener(
                redisMarketCloseEventSubscriber,
                new ChannelTopic("market:close")
        );

        container.addMessageListener(
                redisMarketPhaseEventSubscriber,
                new ChannelTopic("market-phase")
        );

        container.addMessageListener(
                redisGlobalMarketPhaseEventSubscriber,
                new ChannelTopic("market:global-phase")
        );

        container.addMessageListener(
                redisInterestAppliedEventSubscriber,
                new ChannelTopic("account:interest-applied")
        );

        container.addMessageListener(
                redisRankUpdatedEventSubscriber,
                new ChannelTopic("account:rank-updated")
        );

        return container;
    }
}
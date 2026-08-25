package arile.toy.stocksystem.bffserver.stockinfo.config;

import arile.toy.stocksystem.bffserver.stockinfo.event.MarketMainEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.event.MarketMainRedisPublisher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketMainSubscriptionConfig {

    private final RedisMessageListenerContainer container;
    private final MarketMainEventSubscriber subscriber;

    @PostConstruct
    public void subscribe() {
        container.addMessageListener(subscriber, new ChannelTopic(MarketMainRedisPublisher.CHANNEL));
    }
}

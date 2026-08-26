package arile.toy.stocksystem.bffserver.stockinfo.config;

import arile.toy.stocksystem.bffserver.stockinfo.event.GlobalMarketEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.event.GlobalMarketRedisPublisher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GlobalMarketSubscriptionConfig {

    private final RedisMessageListenerContainer container;
    private final GlobalMarketEventSubscriber subscriber;

    @PostConstruct
    public void subscribe() {
        container.addMessageListener(subscriber, new ChannelTopic(GlobalMarketRedisPublisher.CHANNEL));
    }
}
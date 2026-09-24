package arile.toy.stocksystem.bffserver.stockinfo.config;

import arile.toy.stocksystem.bffserver.stockinfo.event.GlobalMarketEventSubscriber;
import arile.toy.stocksystem.bffserver.stockinfo.event.MarketMainEventSubscriber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MarketDataSubscriptionConfigsTest {

    @Test
    @DisplayName("국내 지수 구독자를 market:main:event 채널에 등록한다")
    void marketMain() {
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        MarketMainEventSubscriber subscriber = mock(MarketMainEventSubscriber.class);

        new MarketMainSubscriptionConfig(container, subscriber).subscribe();

        verify(container).addMessageListener(subscriber, new ChannelTopic("market:main:event"));
    }

    @Test
    @DisplayName("환율 구독자를 market:global:event 채널에 등록한다")
    void globalMarket() {
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        GlobalMarketEventSubscriber subscriber = mock(GlobalMarketEventSubscriber.class);

        new GlobalMarketSubscriptionConfig(container, subscriber).subscribe();

        verify(container).addMessageListener(subscriber, new ChannelTopic("market:global:event"));
    }
}

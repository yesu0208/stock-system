package arile.toy.stocksystem.bffserver.config;

import arile.toy.stocksystem.bffserver.external.stock.event.subscriber.RedisStockSummaryEventSubscriber;
import arile.toy.stocksystem.bffserver.market.phase.RedisGlobalMarketPhaseEventSubscriber;
import arile.toy.stocksystem.bffserver.market.phase.RedisInterestAppliedEventSubscriber;
import arile.toy.stocksystem.bffserver.market.phase.RedisMarketCloseEventSubscriber;
import arile.toy.stocksystem.bffserver.market.phase.RedisMarketPhaseEventSubscriber;
import arile.toy.stocksystem.bffserver.market.phase.RedisRankUpdatedEventSubscriber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisSubscriberConfigTest {

    private final RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
    private final RedisStockSummaryEventSubscriber stockSummary = mock(RedisStockSummaryEventSubscriber.class);
    private final RedisMarketCloseEventSubscriber marketClose = mock(RedisMarketCloseEventSubscriber.class);
    private final RedisMarketPhaseEventSubscriber marketPhase = mock(RedisMarketPhaseEventSubscriber.class);
    private final RedisGlobalMarketPhaseEventSubscriber globalPhase = mock(RedisGlobalMarketPhaseEventSubscriber.class);
    private final RedisInterestAppliedEventSubscriber interestApplied = mock(RedisInterestAppliedEventSubscriber.class);
    private final RedisRankUpdatedEventSubscriber rankUpdated = mock(RedisRankUpdatedEventSubscriber.class);

    private final RedisSubscriberConfig config = new RedisSubscriberConfig(
            connectionFactory, stockSummary, marketClose, marketPhase, globalPhase, interestApplied, rankUpdated);

    @Test
    @DisplayName("각 구독자를 약속된 채널에 등록한 리스너 컨테이너를 생성한다")
    @SuppressWarnings("unchecked")
    void redisContainer() {
        RedisMessageListenerContainer container = config.redisContainer();

        assertThat(container.getConnectionFactory()).isSameAs(connectionFactory);

        Map<MessageListener, Collection<Topic>> listenerTopics =
                (Map<MessageListener, Collection<Topic>>) ReflectionTestUtils.getField(container, "listenerTopics");

        assertThat(listenerTopics).hasSize(6);
        assertThat(listenerTopics.get(stockSummary)).containsExactly(new ChannelTopic("summary:event"));
        assertThat(listenerTopics.get(marketClose)).containsExactly(new ChannelTopic("market:close"));
        assertThat(listenerTopics.get(marketPhase)).containsExactly(new ChannelTopic("market-phase"));
        assertThat(listenerTopics.get(globalPhase)).containsExactly(new ChannelTopic("market:global-phase"));
        assertThat(listenerTopics.get(interestApplied)).containsExactly(new ChannelTopic("account:interest-applied"));
        assertThat(listenerTopics.get(rankUpdated)).containsExactly(new ChannelTopic("account:rank-updated"));
    }
}

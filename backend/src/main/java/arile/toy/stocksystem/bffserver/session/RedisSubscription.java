package arile.toy.stocksystem.bffserver.session;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;

public record RedisSubscription(
        ChannelTopic topic,
        MessageListener subscriber
) {
}

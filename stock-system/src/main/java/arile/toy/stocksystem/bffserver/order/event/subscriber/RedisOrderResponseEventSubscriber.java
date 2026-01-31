package arile.toy.stocksystem.bffserver.order.event.subscriber;

import arile.toy.stocksystem.bffserver.order.service.OrderResponsePushService;
import arile.toy.stocksystem.stockserver.trading.event.OrderResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOrderResponseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final OrderResponsePushService orderResponsePushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            OrderResponseEvent event =
                    objectMapper.readValue(
                            body,
                            OrderResponseEvent.class
                    );

            orderResponsePushService.push(event);
        } catch (Exception e) {
            log.warn("OrderResponseEvent:readValue error", e);
        }
    }
}

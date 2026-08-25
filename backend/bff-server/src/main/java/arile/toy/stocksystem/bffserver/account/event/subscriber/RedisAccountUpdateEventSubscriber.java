package arile.toy.stocksystem.bffserver.account.event.subscriber;

import arile.toy.stocksystem.bffserver.account.event.AccountUpdateEvent;
import arile.toy.stocksystem.bffserver.account.service.AccountPushService;
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
public class RedisAccountUpdateEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final AccountPushService accountPushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(
                    message.getBody(),
                    StandardCharsets.UTF_8
            );

            AccountUpdateEvent event =
                    objectMapper.readValue(
                            body,
                            AccountUpdateEvent.class
                    );

            accountPushService.push(event.username());
        } catch (Exception e) {
            log.warn("AccountUpdateEvent:readValue error", e);
        }
    }
}

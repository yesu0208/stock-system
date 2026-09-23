package arile.toy.stocksystem.accountserver.useraccount.event.subscriber;

import arile.toy.stocksystem.accountserver.stream.AbstractRedisStreamConsumer;
import arile.toy.stocksystem.accountserver.useraccount.service.UserAccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUserCreatedEventConsumer extends AbstractRedisStreamConsumer {

    private final UserAccountService userAccountService;

    public RedisUserCreatedEventConsumer(
            RedisTemplate<String, Object> streamRedisTemplate,
            UserAccountService userAccountService,
            @Value("${redis.streams.user.key}") String streamKey,
            @Value("${redis.streams.user.consumer-group}") String group
    ) {
        super(streamRedisTemplate, streamKey, group);
        this.userAccountService = userAccountService;
    }

    @Override
    protected String eventType() {
        return "USER_CREATED";
    }

    @Override
    protected String processedKeyPrefix() {
        return "processed:user-create:";
    }

    @Override
    protected String dlqStreamKey() {
        return "user-create-dlq";
    }

    @Override
    protected void handle(MapRecord<String, Object, Object> record) {
        String username = (String) record.getValue().get("username");

        userAccountService.createAccountIfAbsent(username);

        log.info("Account created for username={}", username);
    }
}

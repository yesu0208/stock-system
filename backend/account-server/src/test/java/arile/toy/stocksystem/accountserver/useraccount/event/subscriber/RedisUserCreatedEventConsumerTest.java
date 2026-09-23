package arile.toy.stocksystem.accountserver.useraccount.event.subscriber;

import arile.toy.stocksystem.accountserver.useraccount.service.UserAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisUserCreatedEventConsumerTest {

    @Mock
    private RedisTemplate<String, Object> streamRedisTemplate;

    @Mock
    private UserAccountService userAccountService;

    private RedisUserCreatedEventConsumer consumer() {
        return new RedisUserCreatedEventConsumer(
                streamRedisTemplate, userAccountService, "user-events", "account-group");
    }

    @Test
    @DisplayName("USER_CREATED 이벤트를 처리하고 전용 processed 키와 DLQ를 사용한다")
    void metadata() {
        RedisUserCreatedEventConsumer consumer = consumer();

        assertThat(consumer.eventType()).isEqualTo("USER_CREATED");
        assertThat(consumer.processedKeyPrefix()).isEqualTo("processed:user-create:");
        assertThat(consumer.dlqStreamKey()).isEqualTo("user-create-dlq");
    }

    @Test
    @DisplayName("handle: 레코드의 username으로 계좌를 생성한다")
    void handle_createsAccount() {
        MapRecord<String, Object, Object> record = StreamRecords.newRecord()
                .in("user-events")
                .withId(RecordId.of("1-0"))
                .ofMap(Map.<Object, Object>of("type", "USER_CREATED", "username", "user1"));

        consumer().handle(record);

        verify(userAccountService).createAccountIfAbsent("user1");
    }
}

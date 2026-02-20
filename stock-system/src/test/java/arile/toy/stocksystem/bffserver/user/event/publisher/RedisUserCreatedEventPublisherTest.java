package arile.toy.stocksystem.bffserver.user.event.publisher;

import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisUserCreatedEventPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private RedisUserCreatedEventPublisher publisher;

    @Captor
    private ArgumentCaptor<MapRecord<String, Object, Object>> captor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        // @Value 필드 주입
        ReflectionTestUtils.setField(publisher, "streamKey", "user-stream");
    }

    @Test
    @DisplayName("USER_CREATED 이벤트를 Redis Stream에 정상 발행")
    void givenUserCreatedEvent_whenPublish_thenRedisStreamReceivesRecord() {

        // given
        UserCreatedEvent event = new UserCreatedEvent("user1");

        when(streamOperations.add(any()))
                .thenReturn(RecordId.of("1-0"));

        // when
        publisher.publishUserCreatedEvent(event);

        // then
        verify(streamOperations).add(captor.capture());

        MapRecord<String, Object, Object> record = captor.getValue();
        Map<Object, Object> value = record.getValue();

        assertThat(record.getStream()).isEqualTo("user-stream");
        assertThat(value).containsEntry("type", "USER_CREATED");
        assertThat(value).containsEntry("username", "user1");
    }
}

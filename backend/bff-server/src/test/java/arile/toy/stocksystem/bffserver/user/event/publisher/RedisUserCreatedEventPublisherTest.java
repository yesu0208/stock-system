package arile.toy.stocksystem.bffserver.user.event.publisher;

import arile.toy.stocksystem.bffserver.user.event.UserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisUserCreatedEventPublisherTest {

    private StreamOperations<String, Object, Object> streamOps;
    private RedisUserCreatedEventPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        streamOps = mock(StreamOperations.class);
        doReturn(streamOps).when(redisTemplate).opsForStream();

        publisher = new RedisUserCreatedEventPublisher(redisTemplate);
        ReflectionTestUtils.setField(publisher, "streamKey", "user-events");
    }

    @Test
    @DisplayName("USER_CREATED 타입과 사용자명을 담아 user-events 스트림에 발행한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void publishUserCreatedEvent() {
        given(streamOps.add(any(MapRecord.class))).willReturn(RecordId.of("1-0"));

        publisher.publishUserCreatedEvent(UserCreatedEvent.of("user1"));

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOps).add(captor.capture());
        MapRecord record = captor.getValue();
        assertThat(record.getStream()).isEqualTo("user-events");
        assertThat((Map<Object, Object>) record.getValue())
                .containsExactlyInAnyOrderEntriesOf(Map.of("type", "USER_CREATED", "username", "user1"));
    }
}

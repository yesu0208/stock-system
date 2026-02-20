package arile.toy.stocksystem.stockserver.useraccount.event.subscriber;

import arile.toy.stocksystem.stockserver.useraccount.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Collections;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.mockito.*;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;

import org.springframework.data.redis.connection.stream.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class RedisUserCreatedEventConsumerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    @Mock
    private UserAccountService userAccountService;

    private RedisUserCreatedEventConsumer consumer;

    private final String streamKey = "user-stream";
    private final String group = "user-group";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);

        consumer = new RedisUserCreatedEventConsumer(
                redisTemplate,
                userAccountService,
                streamKey,
                group
        );
    }

    @Test
    @DisplayName("이벤트 없으면 아무 동작도 하지 않음")
    void givenNoRecords_whenConsume_thenDoNothing() {
        // given
        when(streamOps.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any()
        )).thenReturn(Collections.emptyList());

        // when
        consumer.consume();

        // then
        verify(streamOps).read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any()
        );
        verifyNoInteractions(userAccountService);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("USER_CREATED 이벤트 처리 후 acknowledge")
    void givenUserCreatedRecord_whenConsume_thenCreateAccountAndAck() {
        // given
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "USER_CREATED");
        value.put("username", "testuser");

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);

        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);

        when(streamOps.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any()
        )).thenReturn(List.of(record));

        // when
        consumer.consume();

        // then
        verify(userAccountService).createAccountIfAbsent("testuser");
        verify(streamOps).acknowledge(streamKey, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("USER_CREATED 아닌 이벤트는 무시")
    void givenOtherEvent_whenConsume_thenDoNothing() {
        // given
        Map<Object, Object> value = new HashMap<>();
        value.put("type", "OTHER_EVENT");
        value.put("username", "testuser");

        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);

        RecordId recordId = RecordId.of("1-0");
        when(record.getId()).thenReturn(recordId);

        when(streamOps.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any()
        )).thenReturn(List.of(record));

        // when
        consumer.consume();

        // then
        verifyNoInteractions(userAccountService);
        verify(streamOps).acknowledge(streamKey, group, recordId);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("USER_CREATED 이벤트 직접 호출")
    void givenUserCreatedRecord_whenHandle_thenCreateAccount() throws Exception {
        // given
        Map<Object, Object> value = Map.of(
                "type", "USER_CREATED",
                "username", "user1"
        );
        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn(value);

        // private handle() method
        var handleMethod = RedisUserCreatedEventConsumer.class
                .getDeclaredMethod("handle", MapRecord.class);
        handleMethod.setAccessible(true);

        // when & then
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                handleMethod.invoke(consumer, record)
        );

        verify(userAccountService).createAccountIfAbsent("user1");
    }
}

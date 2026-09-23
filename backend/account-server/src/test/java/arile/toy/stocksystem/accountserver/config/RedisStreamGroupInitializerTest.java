package arile.toy.stocksystem.accountserver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisStreamGroupInitializerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    @InjectMocks
    private RedisStreamGroupInitializer initializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "userStreamKey", "user-events");
        ReflectionTestUtils.setField(initializer, "userConsumerGroup", "account-group");
        ReflectionTestUtils.setField(initializer, "tradeExecutedStreamKey", "trade-executed-events");
        ReflectionTestUtils.setField(initializer, "tradeExecutedConsumerGroup", "account-group");
        doReturn(streamOps).when(redisTemplate).opsForStream();
    }

    @Test
    @DisplayName("user, trade-executed 스트림의 컨슈머 그룹을 생성한다")
    void init_createsBothGroups() {
        initializer.init();

        verify(streamOps).createGroup(eq("user-events"), any(ReadOffset.class), eq("account-group"));
        verify(streamOps).createGroup(eq("trade-executed-events"), any(ReadOffset.class), eq("account-group"));
    }

    @Test
    @DisplayName("그룹이 이미 존재해 예외가 나도 무시하고 다음 스트림을 계속 생성한다")
    void init_whenGroupAlreadyExists_continues() {
        willThrow(new RedisSystemException("BUSYGROUP", null))
                .given(streamOps).createGroup(eq("user-events"), any(ReadOffset.class), eq("account-group"));

        assertThatCode(() -> initializer.init()).doesNotThrowAnyException();

        verify(streamOps).createGroup(eq("trade-executed-events"), any(ReadOffset.class), eq("account-group"));
    }
}

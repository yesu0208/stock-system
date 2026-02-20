package arile.toy.stocksystem.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisStreamGroupInitializerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    private RedisStreamGroupInitializer initializer;

    @BeforeEach
    void setup() {
        initializer = new RedisStreamGroupInitializer(redisTemplate);

        ReflectionTestUtils.setField(initializer, "orderPrefix", "order");
        ReflectionTestUtils.setField(initializer, "orderGroup", "orderGroup");
        ReflectionTestUtils.setField(initializer, "cancelPrefix", "cancel");
        ReflectionTestUtils.setField(initializer, "cancelGroup", "cancelGroup");
        ReflectionTestUtils.setField(initializer, "autoOrderPrefix", "autoOrder");
        ReflectionTestUtils.setField(initializer, "autoOrderGroup", "autoOrderGroup");
        ReflectionTestUtils.setField(initializer, "autoCancelPrefix", "autoCancel");
        ReflectionTestUtils.setField(initializer, "autoCancelGroup", "autoCancelGroup");
        ReflectionTestUtils.setField(initializer, "userStreamKey", "userStream");
        ReflectionTestUtils.setField(initializer, "userGroup", "userGroup");
        ReflectionTestUtils.setField(initializer, "shardIndex", 0);

        when(redisTemplate.opsForStream()).thenReturn(streamOps);
    }

    @Test
    @DisplayName("PostConstruct 시점에 모든 Redis Stream 그룹 생성")
    void givenPostConstruct_whenInit_thenCreateAllGroups() {
        // When
        initializer.init();

        // Then
        verify(streamOps).createGroup("order-0", ReadOffset.latest(), "orderGroup");
        verify(streamOps).createGroup("cancel-0", ReadOffset.latest(), "cancelGroup");
        verify(streamOps).createGroup("autoOrder-0", ReadOffset.latest(), "autoOrderGroup");
        verify(streamOps).createGroup("autoCancel-0", ReadOffset.latest(), "autoCancelGroup");
        verify(streamOps).createGroup("userStream", ReadOffset.latest(), "userGroup");
    }
}

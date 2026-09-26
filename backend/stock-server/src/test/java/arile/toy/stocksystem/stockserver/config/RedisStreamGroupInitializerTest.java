package arile.toy.stocksystem.stockserver.config;

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

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Config] Redis Stream 컨슈머 그룹 초기화 테스트")
@ExtendWith(MockitoExtension.class)
class RedisStreamGroupInitializerTest {

    private static final String[] STREAMS = {"order", "cancel", "auto-order", "auto-cancel", "trailing-stop",
            "trailing-stop-cancel", "otoco", "otoco-cancel", "alert", "alert-cancel"};
    private static final String[] FIELDS = {"order", "cancel", "autoOrder", "autoCancel", "trailingStop",
            "trailingStopCancel", "otoco", "otocoCancel", "alert", "alertCancel"};

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private StreamOperations<String, Object, Object> streamOperations;

    private RedisStreamGroupInitializer sut;

    @BeforeEach
    void setUp() {
        sut = new RedisStreamGroupInitializer(redisTemplate);
        for (int i = 0; i < STREAMS.length; i++) {
            ReflectionTestUtils.setField(sut, FIELDS[i] + "Prefix", STREAMS[i]);
            ReflectionTestUtils.setField(sut, FIELDS[i] + "ConsumerGroup", STREAMS[i] + "-group");
        }
        ReflectionTestUtils.setField(sut, "stockGroup", "A");
        willReturn(streamOperations).given(redisTemplate).opsForStream();
    }

    @DisplayName("10개 요청 스트림({prefix}-{서버 그룹})마다 최신 위치부터 읽는 컨슈머 그룹을 만든다")
    @Test
    void whenInitializing_thenCreatesGroupPerStream() {
        sut.init();

        for (String stream : STREAMS) {
            then(streamOperations).should().createGroup(stream + "-A", ReadOffset.latest(), stream + "-group");
        }
        then(streamOperations).shouldHaveNoMoreInteractions();
    }

    @DisplayName("이미 그룹이 있어 생성이 실패해도 예외 없이 나머지 스트림을 계속 처리한다")
    @Test
    void givenGroupExists_whenInitializing_thenContinues() {
        given(streamOperations.createGroup(anyString(), any(ReadOffset.class), anyString()))
                .willThrow(new IllegalStateException("BUSYGROUP Consumer Group name already exists"));

        assertThatNoException().isThrownBy(sut::init);

        then(streamOperations).should(times(STREAMS.length)).createGroup(anyString(), any(ReadOffset.class), anyString());
    }
}

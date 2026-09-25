package arile.toy.stocksystem.stockserver.market.phase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 종목별 장 상태 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisMarketPhasePublisherTest {

    @InjectMocks private RedisMarketPhasePublisher sut;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOps;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("스냅샷 해시에 종목별 상태를 저장하고, 채널로 상태 변경 이벤트를 발행한다")
    @Test
    void whenPublishing_thenSavesSnapshotAndSendsEvent() throws Exception {
        doReturn(hashOps).when(redisTemplate).opsForHash();

        sut.publish("005930", StockServerMarketPhase.CLOSING_CALL);

        then(hashOps).should().put("market:phase:snapshot", "005930", "CLOSING_CALL");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        then(redisTemplate).should().convertAndSend(eq("market-phase"), captor.capture());
        MarketPhaseEvent sent = objectMapper.readValue(captor.getValue(), MarketPhaseEvent.class);
        assertThat(sent).isEqualTo(MarketPhaseEvent.of("005930", StockServerMarketPhase.CLOSING_CALL));
    }

    @DisplayName("스냅샷 저장이 실패해도 이벤트는 발행한다")
    @Test
    void givenSnapshotFails_whenPublishing_thenStillSendsEvent() {
        given(redisTemplate.opsForHash()).willThrow(new IllegalStateException("redis down"));

        sut.publish("005930", StockServerMarketPhase.OPEN);

        then(redisTemplate).should().convertAndSend(eq("market-phase"), org.mockito.ArgumentMatchers.anyString());
    }

    @DisplayName("이벤트 발행이 실패해도 예외를 밖으로 던지지 않는다")
    @Test
    void givenSendFails_whenPublishing_thenSwallows() {
        doReturn(hashOps).when(redisTemplate).opsForHash();
        given(redisTemplate.convertAndSend(eq("market-phase"), org.mockito.ArgumentMatchers.anyString()))
                .willThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> sut.publish("005930", StockServerMarketPhase.CLOSED)).doesNotThrowAnyException();
    }
}

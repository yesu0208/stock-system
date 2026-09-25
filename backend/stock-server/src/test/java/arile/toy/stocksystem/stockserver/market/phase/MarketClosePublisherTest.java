package arile.toy.stocksystem.stockserver.market.phase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 장 마감 정리 완료 신호 발행 테스트")
@ExtendWith(MockitoExtension.class)
class MarketClosePublisherTest {

    @InjectMocks private MarketClosePublisher sut;

    @Mock private StringRedisTemplate redisTemplate;

    @DisplayName("market:close 채널로 마감 종류(REGULAR/AFTER)를 발행한다")
    @Test
    void whenPublishing_thenSendsCloseType() {
        sut.publishMarketClose("REGULAR");

        then(redisTemplate).should().convertAndSend("market:close", "REGULAR");
    }

    @DisplayName("발행이 실패해도 예외를 밖으로 던지지 않는다")
    @Test
    void givenSendFails_whenPublishing_thenSwallows() {
        given(redisTemplate.convertAndSend("market:close", "AFTER"))
                .willThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> sut.publishMarketClose("AFTER")).doesNotThrowAnyException();
    }
}

package arile.toy.stocksystem.stockserver.alert.event.publisher;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.dto.AlertDto;
import arile.toy.stocksystem.stockserver.alert.dto.AlertErrorCode;
import arile.toy.stocksystem.stockserver.alert.dto.StockServerAlertResponseMessage;
import arile.toy.stocksystem.stockserver.alert.event.AlertFiredEvent;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.stockserver.alert.event.AlertResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 알림 응답·발송 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisAlertResponseEventPublisherTest {

    @Mock private RedisTemplate<String, AlertResponseEvent> alertResponseEventRedisTemplate;
    @Mock private RedisTemplate<String, AlertFiredEvent> alertFiredEventRedisTemplate;

    private RedisAlertResponseEventPublisher sut;

    private final AlertRequestEvent request = AlertRequestEvent.of("user", "005930", AlertDirection.ABOVE, 72_000);
    private final AlertDto dto = new AlertDto(1L, "user", "005930", AlertDirection.ABOVE, 72_000, Instant.now());

    @BeforeEach
    void setUp() {
        // 같은 타입(RedisTemplate) 두 개라 생성자 순서대로 직접 주입
        sut = new RedisAlertResponseEventPublisher(alertResponseEventRedisTemplate, alertFiredEventRedisTemplate);
    }

    @DisplayName("등록 성공·실패는 알림 채널로, 발송은 발송 채널로 발행한다")
    @Test
    void whenPublishing_thenSendsToChannels() {
        var message = StockServerAlertResponseMessage.of(1L, "user", "005930", AlertDirection.ABOVE, 72_000, Instant.now());

        sut.publishRegistered(message);
        sut.publishRegisterError(request, AlertErrorCode.INTERNAL_ERROR);
        sut.publishFired(dto, 72_100);

        then(alertResponseEventRedisTemplate).should().convertAndSend("user:alert.user:event",
                AlertResponseEvent.fromResponseMessage(message));
        then(alertResponseEventRedisTemplate).should().convertAndSend("user:alert.user:event",
                AlertResponseEvent.error(request, AlertErrorCode.INTERNAL_ERROR));
        then(alertFiredEventRedisTemplate).should().convertAndSend(eq("user:alert:fired.user:event"),
                argThat((AlertFiredEvent e) -> e.alertId().equals(1L) && e.currentPrice() == 72_100));
    }

    @DisplayName("Redis 발행이 실패해도 예외를 전파하지 않는다")
    @Test
    void givenRedisFails_whenPublishing_thenSwallows() {
        given(alertResponseEventRedisTemplate.convertAndSend(anyString(), any(AlertResponseEvent.class)))
                .willThrow(new IllegalStateException("redis down"));
        given(alertFiredEventRedisTemplate.convertAndSend(anyString(), any(AlertFiredEvent.class)))
                .willThrow(new IllegalStateException("redis down"));

        assertThatNoException().isThrownBy(() -> {
            sut.publishRegistered(StockServerAlertResponseMessage.of(1L, "user", "005930", AlertDirection.ABOVE, 72_000, Instant.now()));
            sut.publishRegisterError(request, AlertErrorCode.INTERNAL_ERROR);
            sut.publishFired(dto, 72_100);
        });
    }
}

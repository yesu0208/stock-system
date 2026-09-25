package arile.toy.stocksystem.stockserver.trade.event.publisher;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderOrigin;
import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 체결 응답 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisTradeResponseEventPublisherTest {

    @InjectMocks private RedisTradeResponseEventPublisher sut;

    @Mock private RedisTemplate<String, TradeResponseEvent> redisTradeResponseEventRedisTemplate;

    @DisplayName("체결 응답을 체결 당사자 채널로 발행하고, 체결 정보를 그대로 담는다")
    @Test
    void givenEvent_whenPublishing_thenSendsToUserChannel() {
        // Given
        TradeEntity trade = TradeEntity.of(1L, "user", "005930", TradeType.BUY, 70_000, 4,
                LeverageRatio.SPOT, OrderOrigin.MANUAL, null);
        trade.setTradeId(100L);
        var event = TradeResponseEvent.fromEntity(trade);

        // When
        sut.publish(event);

        // Then
        ArgumentCaptor<TradeResponseEvent> captor = ArgumentCaptor.forClass(TradeResponseEvent.class);
        then(redisTradeResponseEventRedisTemplate).should()
                .convertAndSend(eq("user:trade.user:event"), captor.capture());

        TradeResponseEvent sent = captor.getValue();
        assertThat(sent.tradeId()).isEqualTo(100L);
        assertThat(sent.orderId()).isEqualTo(1L);
        assertThat(sent.username()).isEqualTo("user");
        assertThat(sent.stockCode()).isEqualTo("005930");
        assertThat(sent.tradeType()).isEqualTo(TradeType.BUY);
        assertThat(sent.tradePrice()).isEqualTo(70_000);
        assertThat(sent.tradeQuantity()).isEqualTo(4);
    }

    @DisplayName("발행에 실패해도 예외를 밖으로 던지지 않는다 (체결 매칭 중단 방지)")
    @Test
    void givenSendFails_whenPublishing_thenSwallowsException() {
        TradeEntity trade = TradeEntity.of(1L, "user", "005930", TradeType.SELL, 70_000, 4,
                LeverageRatio.SPOT, OrderOrigin.MANUAL, null);
        given(redisTradeResponseEventRedisTemplate.convertAndSend(anyString(), any()))
                .willThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> sut.publish(TradeResponseEvent.fromEntity(trade)))
                .doesNotThrowAnyException();
    }
}

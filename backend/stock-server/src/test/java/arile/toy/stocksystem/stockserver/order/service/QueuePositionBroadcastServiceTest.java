package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderExecutionType;
import arile.toy.stocksystem.stockserver.order.dto.OrderOrigin;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.event.QueuePositionEvent;
import arile.toy.stocksystem.stockserver.order.event.publisher.QueuePositionEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QueuePositionBroadcastServiceTest {

    @Mock private OrderQueueRegistry orderQueueRegistry;
    @Mock private QueuePositionEventPublisher queuePositionEventPublisher;

    @InjectMocks
    private QueuePositionBroadcastService service;

    /** 주문 수량은 100으로 고정하고 남은 수량만 바꿔, 계산이 남은 수량 기준인지 확인 */
    private static OrderDto order(long orderId, String username, int remainingQuantity) {
        return new OrderDto(orderId, username, "005930", OrderType.BUY, LeverageRatio.SPOT,
                70_000, 100, remainingQuantity, OrderStatus.PARTIAL, Instant.parse("2026-09-24T00:00:00Z"),
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);
    }

    @Test
    @DisplayName("우선순위대로 각 주문의 주인에게, 자기보다 앞선 주문들의 남은 수량 합을 보낸다")
    void quantityAhead_isSumOfRemainingAhead() {
        given(orderQueueRegistry.snapshotRanked("005930", OrderType.BUY)).willReturn(List.of(
                order(1, "userA", 100),
                order(2, "userB", 50),
                order(3, "userC", 30)));

        service.broadcast("005930", OrderType.BUY);

        ArgumentCaptor<QueuePositionEvent> events = ArgumentCaptor.forClass(QueuePositionEvent.class);
        verify(queuePositionEventPublisher, times(3)).publish(events.capture());
        assertThat(events.getAllValues())
                .extracting(QueuePositionEvent::orderId, QueuePositionEvent::username,
                        QueuePositionEvent::stockCode, QueuePositionEvent::quantityAhead)
                .containsExactly(
                        tuple(1L, "userA", "005930", 0L),
                        tuple(2L, "userB", "005930", 100L),
                        tuple(3L, "userC", "005930", 150L));
    }

    @Test
    @DisplayName("한 주문의 발행이 실패해도 예외를 던지지 않고, 그 주문의 잔량까지 더해 뒤 주문들에게 계속 보낸다")
    void publishFails_continuesWithCorrectSum() {
        given(orderQueueRegistry.snapshotRanked("005930", OrderType.BUY)).willReturn(List.of(
                order(1, "userA", 100),
                order(2, "userB", 50),
                order(3, "userC", 30)));
        willThrow(new RuntimeException("redis down"))
                .given(queuePositionEventPublisher).publish(argThat(e -> e.orderId().equals(2L)));

        assertThatCode(() -> service.broadcast("005930", OrderType.BUY)).doesNotThrowAnyException();

        verify(queuePositionEventPublisher).publish(argThat(e -> e.orderId().equals(3L) && e.quantityAhead() == 150L));
    }

    @Test
    @DisplayName("대기 중인 주문이 없으면 아무것도 보내지 않는다")
    void emptyQueue() {
        given(orderQueueRegistry.snapshotRanked("005930", OrderType.SELL)).willReturn(List.of());

        service.broadcast("005930", OrderType.SELL);

        verifyNoInteractions(queuePositionEventPublisher);
    }
}

package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.order.dto.OrderExecutionType;
import arile.toy.stocksystem.bffserver.order.dto.OrderRequest;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponse;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.event.OrderRequestEvent;
import arile.toy.stocksystem.bffserver.order.event.publisher.OrderRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderIngressServiceTest {

    @Mock
    private OrderRequestEventPublisher orderRequestEventPublisher;

    @InjectMocks
    private OrderIngressService service;

    @Test
    @DisplayName("레버리지·체결 방식 기본값(SPOT·LIMIT)을 채워 주문 이벤트를 발행하고 접수 내용을 반환한다")
    void receive_withDefaults() {
        OrderResponse response = service.receive("user1",
                new OrderRequest("005930", OrderType.BUY, 70_000, 10, null, null));

        verify(orderRequestEventPublisher).publishOrder(new OrderRequestEvent(
                "user1", "005930", OrderType.BUY, 70_000, 10, LeverageRatio.SPOT, OrderExecutionType.LIMIT));
        assertThat(response).isEqualTo(
                new OrderResponse("user1", "005930", OrderType.BUY, 70_000, 10, LeverageRatio.SPOT));
    }

    @Test
    @DisplayName("지정한 레버리지 배율과 체결 방식을 그대로 이벤트에 담는다")
    void receive_withExplicitValues() {
        OrderResponse response = service.receive("user1",
                new OrderRequest("005930", OrderType.SELL, 71_000, 3, LeverageRatio.X2, OrderExecutionType.MARKET));

        verify(orderRequestEventPublisher).publishOrder(new OrderRequestEvent(
                "user1", "005930", OrderType.SELL, 71_000, 3, LeverageRatio.X2, OrderExecutionType.MARKET));
        assertThat(response.leverageRatio()).isEqualTo(LeverageRatio.X2);
    }
}

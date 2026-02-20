package arile.toy.stocksystem.bffserver.order.service;

import arile.toy.stocksystem.bffserver.order.dto.OrderRequest;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponse;
import arile.toy.stocksystem.bffserver.order.dto.OrderType;
import arile.toy.stocksystem.bffserver.order.event.publisher.OrderRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderIngressServiceTest {

    @Mock
    private OrderRequestEventPublisher orderRequestEventPublisher;

    @InjectMocks
    private OrderIngressService service;

    @Test
    @DisplayName("OrderRequest 처리 시 이벤트 발행 및 OrderResponse 반환")
    void givenOrderRequest_whenReceive_thenPublishesEventAndReturnsResponse() {
        // given
        String username = "user1";
        OrderRequest request = new OrderRequest("005930", OrderType.BUY, 50000, 10);

        // when
        OrderResponse response = service.receive(username, request);

        // then
        verify(orderRequestEventPublisher).publishOrder(argThat(event ->
                event.username().equals(username) &&
                        event.stockCode().equals(request.stockCode()) &&
                        event.orderType() == request.orderType() &&
                        event.orderPrice() == request.orderPrice() &&
                        event.orderQuantity() == request.orderQuantity()
        ));

        assertEquals(username, response.username());
        assertEquals(request.stockCode(), response.stockCode());
        assertEquals(request.orderType(), response.orderType());
        assertEquals(request.orderPrice(), response.orderPrice());
        assertEquals(request.orderQuantity(), response.orderQuantity());
    }
}

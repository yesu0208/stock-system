package arile.toy.stocksystem.bffserver.autoorder.service;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderRequest;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponse;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderRequestEvent;
import arile.toy.stocksystem.bffserver.autoorder.event.publisher.AutoOrderRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoOrderIngressServiceTest {

    @Mock
    private AutoOrderRequestEventPublisher publisher;

    @InjectMocks
    private AutoOrderIngressService service;

    @Test
    @DisplayName("AutoOrderRequest 수신 시 이벤트 발행하고 올바른 Response 반환")
    void givenAutoOrderRequest_whenReceive_thenPublishesEventAndReturnsResponse() {
        // given
        String username = "user1";
        AutoOrderRequest request = new AutoOrderRequest(
                "005930",
                AutoOrderType.BUY,
                50000,
                50000,
                50
        );

        // when
        AutoOrderResponse response = service.receive(username, request);

        // then
        verify(publisher).publishAutoOrder(any(AutoOrderRequestEvent.class));

        assertThat(response.username()).isEqualTo(username);
        assertThat(response.stockCode()).isEqualTo(request.stockCode());
        assertThat(response.autoOrderType()).isEqualTo(request.autoOrderType());
        assertThat(response.triggerPrice()).isEqualTo(request.triggerPrice());
        assertThat(response.orderPrice()).isEqualTo(request.orderPrice());
        assertThat(response.orderQuantity()).isEqualTo(request.orderQuantity());
    }
}

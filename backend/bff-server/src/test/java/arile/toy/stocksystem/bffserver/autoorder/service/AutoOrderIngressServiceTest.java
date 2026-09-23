package arile.toy.stocksystem.bffserver.autoorder.service;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderRequest;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponse;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderRequestEvent;
import arile.toy.stocksystem.bffserver.autoorder.event.publisher.AutoOrderRequestEventPublisher;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoOrderIngressServiceTest {

    @Mock
    private AutoOrderRequestEventPublisher publisher;

    @InjectMocks
    private AutoOrderIngressService service;

    @Test
    @DisplayName("레버리지 기본값(SPOT)을 채워 자동 주문 이벤트를 발행하고 접수 내용을 반환한다")
    void receive() {
        AutoOrderResponse response = service.receive("user1",
                new AutoOrderRequest("005930", AutoOrderType.SELL, 72_000, 71_500, 5, null));

        verify(publisher).publishAutoOrder(new AutoOrderRequestEvent(
                "user1", "005930", AutoOrderType.SELL, 72_000, 71_500, 5, LeverageRatio.SPOT));
        assertThat(response).isEqualTo(new AutoOrderResponse(
                "user1", "005930", AutoOrderType.SELL, 72_000, 71_500, 5, LeverageRatio.SPOT));
    }
}

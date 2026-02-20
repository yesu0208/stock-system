package arile.toy.stocksystem.bffserver.cancel.service;

import arile.toy.stocksystem.bffserver.cancel.dto.CancelRequest;
import arile.toy.stocksystem.bffserver.cancel.dto.CancelResponse;
import arile.toy.stocksystem.bffserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.bffserver.cancel.event.publisher.CancelRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CancelIngressServiceTest {

    @Mock
    private CancelRequestEventPublisher publisher;

    @InjectMocks
    private CancelIngressService service;

    @Test
    @DisplayName("CancelRequest 수신 시 이벤트 발행 후 올바른 Response 반환")
    void givenCancelRequest_whenReceive_thenPublishesEventAndReturnsResponse() {
        // given
        CancelRequest request = new CancelRequest(1L, "005930");

        // when
        CancelResponse response = service.receive(request);

        // then
        verify(publisher).publishCancel(any(CancelRequestEvent.class));

        assertThat(response.orderId()).isEqualTo(request.orderId());
        assertThat(response.stockCode()).isEqualTo(request.stockCode());
    }
}

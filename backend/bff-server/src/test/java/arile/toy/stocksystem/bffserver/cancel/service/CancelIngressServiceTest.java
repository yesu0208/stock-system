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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CancelIngressServiceTest {

    @Mock
    private CancelRequestEventPublisher publisher;

    @InjectMocks
    private CancelIngressService service;

    @Test
    @DisplayName("요청자 사용자명을 담은 취소 이벤트를 발행하고, 주문 ID·종목코드를 응답한다")
    void receive() {
        CancelResponse response = service.receive("user1", new CancelRequest(1L, "005930"));

        verify(publisher).publishCancel(new CancelRequestEvent(1L, "005930", "user1"));
        assertThat(response).isEqualTo(new CancelResponse(1L, "005930"));
    }
}

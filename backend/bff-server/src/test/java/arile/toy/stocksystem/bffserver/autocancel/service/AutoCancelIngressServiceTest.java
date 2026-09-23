package arile.toy.stocksystem.bffserver.autocancel.service;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelRequest;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelResponse;
import arile.toy.stocksystem.bffserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.bffserver.autocancel.event.publisher.RedisAutoCancelRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoCancelIngressServiceTest {

    @Mock
    private RedisAutoCancelRequestEventPublisher publisher;

    @InjectMocks
    private AutoCancelIngressService service;

    @Test
    @DisplayName("요청자 사용자명을 담은 자동 주문 취소 이벤트를 발행하고, ID·종목코드를 응답한다")
    void receive() {
        AutoCancelResponse response = service.receive("user1", new AutoCancelRequest(1L, "005930"));

        verify(publisher).publishAutoCancel(new AutoCancelRequestEvent(1L, "005930", "user1"));
        assertThat(response).isEqualTo(new AutoCancelResponse(1L, "005930"));
    }
}

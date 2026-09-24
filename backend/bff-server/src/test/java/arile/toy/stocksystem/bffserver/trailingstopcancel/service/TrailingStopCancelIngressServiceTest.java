package arile.toy.stocksystem.bffserver.trailingstopcancel.service;

import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelRequest;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelResponse;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.TrailingStopCancelRequestEvent;
import arile.toy.stocksystem.bffserver.trailingstopcancel.event.publisher.TrailingStopCancelRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrailingStopCancelIngressServiceTest {

    @Mock
    private TrailingStopCancelRequestEventPublisher publisher;

    @InjectMocks
    private TrailingStopCancelIngressService service;

    @Test
    @DisplayName("요청자 사용자명을 담은 트레일링 스탑 취소 이벤트를 발행하고, ID·종목코드를 응답한다")
    void receive() {
        TrailingStopCancelResponse response =
                service.receive("user1", new TrailingStopCancelRequest(1L, "005930"));

        verify(publisher).publishTrailingStopCancel(new TrailingStopCancelRequestEvent(1L, "005930", "user1"));
        assertThat(response).isEqualTo(new TrailingStopCancelResponse(1L, "005930"));
    }
}

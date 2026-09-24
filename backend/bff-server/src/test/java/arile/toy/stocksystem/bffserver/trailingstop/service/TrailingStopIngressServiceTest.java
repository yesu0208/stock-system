package arile.toy.stocksystem.bffserver.trailingstop.service;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopRequest;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponse;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.bffserver.trailingstop.event.TrailingStopRequestEvent;
import arile.toy.stocksystem.bffserver.trailingstop.event.publisher.TrailingStopRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrailingStopIngressServiceTest {

    @Mock
    private TrailingStopRequestEventPublisher publisher;

    @InjectMocks
    private TrailingStopIngressService service;

    @Test
    @DisplayName("레버리지 기본값(SPOT)을 채워 트레일링 스탑 이벤트를 발행하고 접수 내용을 반환한다")
    void receive() {
        TrailingStopResponse response = service.receive("user1",
                new TrailingStopRequest("005930", TrailingStopType.BUY, 5, 2.5, 70_000, null));

        verify(publisher).publishTrailingStop(new TrailingStopRequestEvent(
                "user1", "005930", TrailingStopType.BUY, 5, 2.5, 70_000, LeverageRatio.SPOT));
        assertThat(response).isEqualTo(new TrailingStopResponse(
                "user1", "005930", TrailingStopType.BUY, 5, 2.5, 70_000, LeverageRatio.SPOT));
    }
}

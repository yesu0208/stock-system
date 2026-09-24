package arile.toy.stocksystem.bffserver.otoco.service;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoExitMode;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoRequest;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponse;
import arile.toy.stocksystem.bffserver.otoco.event.OtocoRequestEvent;
import arile.toy.stocksystem.bffserver.otoco.event.publisher.OtocoRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OtocoIngressServiceTest {

    @Mock
    private OtocoRequestEventPublisher publisher;

    @InjectMocks
    private OtocoIngressService service;

    @Test
    @DisplayName("레버리지 기본값(SPOT)을 채워 OTOCO 이벤트를 발행하고, 요청 내용 그대로 접수 응답을 반환한다")
    void receive() {
        OtocoResponse response = service.receive("user1", new OtocoRequest(
                "005930", OtocoEntryDirection.BELOW, 10, 70_000,
                OtocoExitMode.PCT, null, 5.0, OtocoExitMode.PRICE, 67_000, null, null));

        verify(publisher).publishOtoco(new OtocoRequestEvent(
                "user1", "005930", OtocoEntryDirection.BELOW, 10, 70_000,
                OtocoExitMode.PCT, null, 5.0, OtocoExitMode.PRICE, 67_000, null, LeverageRatio.SPOT));
        assertThat(response).isEqualTo(new OtocoResponse(
                "user1", "005930", OtocoEntryDirection.BELOW, 10, 70_000,
                OtocoExitMode.PCT, null, 5.0, OtocoExitMode.PRICE, 67_000, null, LeverageRatio.SPOT));
    }
}

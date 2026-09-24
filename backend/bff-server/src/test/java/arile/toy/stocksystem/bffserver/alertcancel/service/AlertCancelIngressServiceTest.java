package arile.toy.stocksystem.bffserver.alertcancel.service;

import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelRequest;
import arile.toy.stocksystem.bffserver.alertcancel.dto.AlertCancelResponse;
import arile.toy.stocksystem.bffserver.alertcancel.event.AlertCancelRequestEvent;
import arile.toy.stocksystem.bffserver.alertcancel.event.publisher.AlertCancelRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertCancelIngressServiceTest {

    @Mock
    private AlertCancelRequestEventPublisher publisher;

    @InjectMocks
    private AlertCancelIngressService service;

    @Test
    @DisplayName("요청자 사용자명을 담은 알림 취소 이벤트를 발행하고, ID·종목코드를 응답한다")
    void receive() {
        AlertCancelResponse response = service.receive("user1", new AlertCancelRequest(1L, "005930"));

        verify(publisher).publishAlertCancel(new AlertCancelRequestEvent(1L, "005930", "user1"));
        assertThat(response).isEqualTo(new AlertCancelResponse(1L, "005930"));
    }
}

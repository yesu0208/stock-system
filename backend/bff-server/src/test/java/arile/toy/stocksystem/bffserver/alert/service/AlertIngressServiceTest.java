package arile.toy.stocksystem.bffserver.alert.service;

import arile.toy.stocksystem.bffserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.bffserver.alert.dto.AlertRequest;
import arile.toy.stocksystem.bffserver.alert.dto.AlertResponse;
import arile.toy.stocksystem.bffserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.bffserver.alert.event.publisher.AlertRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertIngressServiceTest {

    @Mock
    private AlertRequestEventPublisher publisher;

    @InjectMocks
    private AlertIngressService service;

    @Test
    @DisplayName("요청자 사용자명을 담은 알림 등록 이벤트를 발행하고, 요청 내용 그대로 접수 응답을 반환한다")
    void receive() {
        AlertResponse response = service.receive("user1", new AlertRequest("005930", AlertDirection.BELOW, 65_000));

        verify(publisher).publishAlert(new AlertRequestEvent("user1", "005930", AlertDirection.BELOW, 65_000));
        assertThat(response).isEqualTo(new AlertResponse("user1", "005930", AlertDirection.BELOW, 65_000));
    }
}

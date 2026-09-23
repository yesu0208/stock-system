package arile.toy.stocksystem.bffserver.otococancel.service;

import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelRequest;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelResponse;
import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.bffserver.otococancel.event.publisher.OtocoCancelRequestEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OtocoCancelIngressServiceTest {

    @Mock
    private OtocoCancelRequestEventPublisher publisher;

    @InjectMocks
    private OtocoCancelIngressService service;

    @Test
    @DisplayName("요청자 사용자명을 담은 OTOCO 취소 이벤트를 발행하고, ID·종목코드를 응답한다")
    void receive() {
        OtocoCancelResponse response = service.receive("user1", new OtocoCancelRequest(1L, "005930"));

        verify(publisher).publishOtocoCancel(new OtocoCancelRequestEvent(1L, "005930", "user1"));
        assertThat(response).isEqualTo(new OtocoCancelResponse(1L, "005930"));
    }
}

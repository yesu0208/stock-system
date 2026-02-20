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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoCancelIngressServiceTest {

    @Mock
    private RedisAutoCancelRequestEventPublisher publisher;

    @InjectMocks
    private AutoCancelIngressService service;

    @Test
    @DisplayName("AutoCancelRequest 수신 시 이벤트 발행하고 올바른 Response 반환")
    void givenAutoCancelRequest_whenReceive_thenPublishesEventAndReturnsResponse() {
        // given
        AutoCancelRequest request = new AutoCancelRequest(1L, "005930");

        // when
        AutoCancelResponse response = service.receive(request);

        // then
        verify(publisher).publishAutoCancel(any(AutoCancelRequestEvent.class));

        assertThat(response.autoOrderId()).isEqualTo(1L);
        assertThat(response.stockCode()).isEqualTo("005930");
    }
}

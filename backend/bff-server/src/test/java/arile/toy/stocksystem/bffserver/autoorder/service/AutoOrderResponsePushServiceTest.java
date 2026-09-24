package arile.toy.stocksystem.bffserver.autoorder.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResultCode;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResultResponse;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.bffserver.autoorder.event.AutoOrderResponseEvent;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AutoOrderResponsePushServiceTest {

    private static final Instant ORDER_TIME = Instant.parse("2026-09-24T00:30:00Z");

    @Mock private BffServerAutoOrderResponseRepository bffServerAutoOrderResponseRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AutoOrderResponsePushService service;

    private static AutoOrderResponseEvent event(Long autoOrderId, boolean success, AutoOrderResultCode resultCode) {
        return new AutoOrderResponseEvent(autoOrderId, "user1", "005930", AutoOrderType.BUY, LeverageRatio.X1_5,
                68_000, 69_000, 10, ORDER_TIME, success, resultCode);
    }

    @Test
    @DisplayName("등록 성공: 자동 주문 ID·접수 시각을 담은 SUCCESS 결과를 먼저 보내고, 이어서 자동 주문 목록을 보낸다")
    void registered() {
        List<AutoOrderResponseMessage> pending = List.of(mock(AutoOrderResponseMessage.class));
        given(bffServerAutoOrderResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event(1L, true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/order/result",
                AutoOrderResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930", AutoOrderType.BUY,
                        LeverageRatio.X1_5, 68_000, 69_000, 10, ORDER_TIME, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/order", pending);
    }

    @Test
    @DisplayName("발동: 결과 메시지 없이 발동된 주문이 빠진 자동 주문 목록만 보낸다 (이후 일반 주문 알림과 중복 방지)")
    void triggered() {
        List<AutoOrderResponseMessage> pending = List.of();
        given(bffServerAutoOrderResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event(1L, true, AutoOrderResultCode.TRIGGERED));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/order", pending);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), eq("/sub/auto/order/result"), any());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AutoOrderResultCode.class, names = "TRIGGERED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("실패: 결과 코드의 안내 문구를 담은 ERROR 결과만 보내고, 목록은 다시 조회하지 않는다")
    void failure(AutoOrderResultCode resultCode) {
        service.push(event(null, false, resultCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/order/result",
                AutoOrderResultResponse.of(ResponseType.ERROR, null, "user1", "005930", AutoOrderType.BUY,
                        LeverageRatio.X1_5, 68_000, 69_000, 10, null, resultCode.userMessage()));
        verifyNoInteractions(bffServerAutoOrderResponseRepository);
    }

    @Test
    @DisplayName("실패인데 결과 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(null, false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/auto/order/result",
                AutoOrderResultResponse.of(ResponseType.ERROR, null, "user1", "005930", AutoOrderType.BUY,
                        LeverageRatio.X1_5, 68_000, 69_000, 10, null, "알 수 없는 오류가 발생했습니다."));
    }
}

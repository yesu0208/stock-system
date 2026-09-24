package arile.toy.stocksystem.bffserver.trailingstop.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponseMessage;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResultCode;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResultResponse;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.bffserver.trailingstop.event.TrailingStopResponseEvent;
import arile.toy.stocksystem.bffserver.trailingstop.repository.BffServerTrailingStopResponseRepository;
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
class TrailingStopResponsePushServiceTest {

    private static final Instant ORDER_TIME = Instant.parse("2026-09-24T00:30:00Z");

    @Mock private BffServerTrailingStopResponseRepository bffServerTrailingStopResponseRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TrailingStopResponsePushService service;

    private static TrailingStopResponseEvent event(boolean success, TrailingStopResultCode resultCode) {
        return new TrailingStopResponseEvent(1L, "user1", "005930", TrailingStopType.SELL, LeverageRatio.SPOT,
                10, 3.0, 72_000, 69_800, ORDER_TIME, success, resultCode);
    }

    @Test
    @DisplayName("등록 성공: SUCCESS 결과(발동가 포함)를 먼저 보내고, 이어서 트레일링 스탑 목록을 보낸다")
    void registered() {
        List<TrailingStopResponseMessage> pending = List.of(mock(TrailingStopResponseMessage.class));
        given(bffServerTrailingStopResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event(true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop/result",
                TrailingStopResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930", TrailingStopType.SELL,
                        LeverageRatio.SPOT, 10, 3.0, 72_000, 69_800, ORDER_TIME, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop", pending);
    }

    @Test
    @DisplayName("발동: 결과 메시지 없이 발동된 주문이 빠진 목록만 보낸다")
    void triggered() {
        List<TrailingStopResponseMessage> pending = List.of();
        given(bffServerTrailingStopResponseRepository.findAll("user1")).willReturn(pending);

        service.push(event(true, TrailingStopResultCode.TRIGGERED));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop", pending);
        verify(messagingTemplate, never())
                .convertAndSendToUser(anyString(), eq("/sub/trailing-stop/result"), any());
    }

    @Test
    @DisplayName("기준가 갱신: 이벤트 값으로 만든 한 건만 /update 채널로 보내고, 목록은 조회하지 않는다")
    void trailingUpdated() {
        service.push(event(true, TrailingStopResultCode.TRAILING_UPDATED));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop/update",
                new TrailingStopResponseMessage(1L, "user1", "005930", TrailingStopType.SELL, LeverageRatio.SPOT,
                        10, 3.0, 72_000, 69_800, ORDER_TIME));
        verifyNoInteractions(bffServerTrailingStopResponseRepository);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = TrailingStopResultCode.class, names = {"TRIGGERED", "TRAILING_UPDATED"},
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("실패: 결과 코드의 안내 문구를 담은 ERROR 결과만 보내고, 목록은 조회하지 않는다")
    void failure(TrailingStopResultCode resultCode) {
        service.push(event(false, resultCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop/result",
                TrailingStopResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930", TrailingStopType.SELL,
                        LeverageRatio.SPOT, 10, 3.0, 72_000, 69_800, ORDER_TIME, resultCode.userMessage()));
        verifyNoInteractions(bffServerTrailingStopResponseRepository);
    }

    @Test
    @DisplayName("실패인데 결과 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/trailing-stop/result",
                TrailingStopResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930", TrailingStopType.SELL,
                        LeverageRatio.SPOT, 10, 3.0, 72_000, 69_800, ORDER_TIME, "알 수 없는 오류가 발생했습니다."));
    }
}

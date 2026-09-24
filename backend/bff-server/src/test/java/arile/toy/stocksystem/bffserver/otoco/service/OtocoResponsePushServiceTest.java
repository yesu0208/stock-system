package arile.toy.stocksystem.bffserver.otoco.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponseMessage;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResultResponse;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.bffserver.otoco.event.OtocoResponseEvent;
import arile.toy.stocksystem.bffserver.otoco.repository.BffServerOtocoResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OtocoResponsePushServiceTest {

    private static final Instant ORDER_TIME = Instant.parse("2026-09-24T00:30:00Z");

    @Mock private BffServerOtocoResponseRepository bffServerOtocoResponseRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private OtocoResponsePushService service;

    private static OtocoResponseEvent event(boolean success, OtocoResultCode resultCode, OtocoStatus status) {
        return new OtocoResponseEvent(1L, "user1", "005930", OtocoEntryDirection.ABOVE, LeverageRatio.SPOT,
                10, 70_000, 75_000, 65_000, status, ORDER_TIME, success, resultCode, 4);
    }

    private static OtocoResultResponse result(ResponseType type, OtocoStatus status, String errorMessage) {
        return OtocoResultResponse.of(type, 1L, "user1", "005930", OtocoEntryDirection.ABOVE, LeverageRatio.SPOT,
                10, 70_000, 75_000, 65_000, status, ORDER_TIME, errorMessage);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OtocoResultCode.class, names = {
            "INSUFFICIENT_BALANCE", "INTERNAL_ERROR", "INVALID_TP_PRICE", "INVALID_SL_PRICE", "ENTRY_FAILED"})
    @DisplayName("실패: 결과 코드의 안내 문구를 담은 ERROR 결과만 보내고, 목록은 조회하지 않는다")
    void failure(OtocoResultCode resultCode) {
        service.push(event(false, resultCode, OtocoStatus.CANCELED));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco/result",
                result(ResponseType.ERROR, OtocoStatus.CANCELED, resultCode.userMessage()));
        verifyNoInteractions(bffServerOtocoResponseRepository);
    }

    @Test
    @DisplayName("실패인데 결과 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null, OtocoStatus.CANCELED));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco/result",
                result(ResponseType.ERROR, OtocoStatus.CANCELED, "알 수 없는 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("진입 부분 체결: 남은 진입 수량을 담은 한 건만 /update 채널로 보내고, 목록은 조회하지 않는다")
    void entryPartiallyFilled() {
        service.push(event(true, OtocoResultCode.ENTRY_PARTIALLY_FILLED, OtocoStatus.ENTRY_ORDER_PLACED));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco/update",
                new OtocoResponseMessage(1L, "user1", "005930", OtocoEntryDirection.ABOVE, LeverageRatio.SPOT,
                        10, 70_000, 75_000, 65_000, OtocoStatus.ENTRY_ORDER_PLACED, ORDER_TIME, 4));
        verifyNoInteractions(bffServerOtocoResponseRepository);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OtocoResultCode.class, names = {
            "ENTRY_TRIGGERED", "ENTRY_FILLED", "TP_TRIGGERED", "SL_TRIGGERED", "ENTRY_CANCELED"})
    @NullSource
    @DisplayName("단계 전환·진입 취소·신규 등록: 현재 상태를 담은 SUCCESS 결과를 먼저 보내고, 이어서 목록을 보낸다")
    void stageChangeOrRegistered(OtocoResultCode resultCode) {
        List<OtocoResponseMessage> otocos = List.of(mock(OtocoResponseMessage.class));
        given(bffServerOtocoResponseRepository.findAll("user1")).willReturn(otocos);

        service.push(event(true, resultCode, OtocoStatus.WAITING_EXIT));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco/result",
                result(ResponseType.SUCCESS, OtocoStatus.WAITING_EXIT, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco", otocos);
    }
}

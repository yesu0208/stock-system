package arile.toy.stocksystem.bffserver.otococancel.service;

import arile.toy.stocksystem.bffserver.ResponseType;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponseMessage;
import arile.toy.stocksystem.bffserver.otoco.repository.BffServerOtocoResponseRepository;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelErrorCode;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelResultResponse;
import arile.toy.stocksystem.bffserver.otococancel.event.OtocoCancelResponseEvent;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OtocoCancelResponsePushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerOtocoResponseRepository bffServerOtocoResponseRepository;

    @InjectMocks
    private OtocoCancelResponsePushService service;

    private static OtocoCancelResponseEvent event(boolean success, OtocoCancelErrorCode errorCode) {
        OtocoCancelResponseEvent event = mock(OtocoCancelResponseEvent.class);
        given(event.otocoId()).willReturn(1L);
        given(event.username()).willReturn("user1");
        given(event.stockCode()).willReturn("005930");
        given(event.entryDirection()).willReturn(OtocoEntryDirection.BELOW);
        given(event.entryTriggerPrice()).willReturn(68_000);
        given(event.orderQuantity()).willReturn(10);
        given(event.success()).willReturn(success);
        if (!success) {
            given(event.errorCode()).willReturn(errorCode);
        }
        return event;
    }

    @Test
    @DisplayName("성공: SUCCESS 결과를 먼저 보내고, 이어서 취소가 반영된 OTOCO 목록을 보낸다")
    void success() {
        List<OtocoResponseMessage> otocos = List.of(mock(OtocoResponseMessage.class));
        given(bffServerOtocoResponseRepository.findAll("user1")).willReturn(otocos);

        service.push(event(true, null));

        InOrder inOrder = inOrder(messagingTemplate);
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco/cancel",
                OtocoCancelResultResponse.of(ResponseType.SUCCESS, 1L, "user1", "005930",
                        OtocoEntryDirection.BELOW, 68_000, 10, null));
        inOrder.verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco", otocos);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OtocoCancelErrorCode.class)
    @DisplayName("실패: OTOCO ID와 오류 안내 문구를 담은 ERROR 결과만 보내고, 목록은 조회하지 않는다")
    void failure(OtocoCancelErrorCode errorCode) {
        service.push(event(false, errorCode));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco/cancel",
                OtocoCancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930",
                        OtocoEntryDirection.BELOW, 68_000, 10, errorCode.userMessage()));
        verifyNoInteractions(bffServerOtocoResponseRepository);
    }

    @Test
    @DisplayName("실패인데 오류 코드가 없으면 기본 안내 문구를 보낸다")
    void failureWithoutCode() {
        service.push(event(false, null));

        verify(messagingTemplate).convertAndSendToUser("user1", "/sub/otoco/cancel",
                OtocoCancelResultResponse.of(ResponseType.ERROR, 1L, "user1", "005930",
                        OtocoEntryDirection.BELOW, 68_000, 10, "알 수 없는 오류가 발생했습니다."));
    }
}

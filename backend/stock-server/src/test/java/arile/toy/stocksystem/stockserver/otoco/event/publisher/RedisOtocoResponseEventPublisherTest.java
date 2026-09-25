package arile.toy.stocksystem.stockserver.otoco.event.publisher;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.event.OtocoResponseEvent;
import arile.toy.stocksystem.stockserver.otoco.event.StockServerOtocoRequestEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] OTOCO 응답 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class RedisOtocoResponseEventPublisherTest {

    private static final String CHANNEL = "user:otoco.user:event";
    private static final Instant ORDER_TIME = Instant.parse("2026-09-25T00:00:00Z");

    @InjectMocks private RedisOtocoResponseEventPublisher sut;
    @Mock private RedisTemplate<String, OtocoResponseEvent> redisTemplate;

    @DisplayName("등록 응답: 메시지 값을 성공 이벤트로 발행한다")
    @Test
    void whenPublishing_thenSendsSuccess() {
        sut.publish(new StockServerOtocoResponseMessage(1L, "user", "005930", OtocoEntryDirection.BELOW,
                LeverageRatio.SPOT, 10, 70_000, 73_500, 67_900, OtocoStatus.WAITING_ENTRY, null, ORDER_TIME, null));

        OtocoResponseEvent event = captured().get(0);
        assertThat(event.otocoId()).isEqualTo(1L);
        assertThat(event.tpTriggerPrice()).isEqualTo(73_500);
        assertThat(event.otocoStatus()).isEqualTo(OtocoStatus.WAITING_ENTRY);
        assertThat(event.success()).isTrue();
        assertThat(event.resultCode()).isNull();
    }

    @DisplayName("등록 실패: 요청 값과 CANCELED 상태·결과 코드로 발행한다")
    @Test
    void whenPublishingError_thenSendsFailure() {
        sut.publishError(new StockServerOtocoRequestEvent("user", "005930", OtocoEntryDirection.ABOVE, 10, 70_000,
                        OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 67_900, null, LeverageRatio.X2),
                OtocoResultCode.INVALID_TP_PRICE);

        OtocoResponseEvent event = captured().get(0);
        assertThat(event.otocoId()).isNull();
        assertThat(event.entryTriggerPrice()).isEqualTo(70_000);
        assertThat(event.tpTriggerPrice()).isNull();
        assertThat(event.otocoStatus()).isEqualTo(OtocoStatus.CANCELED);
        assertThat(event.success()).isFalse();
        assertThat(event.resultCode()).isEqualTo(OtocoResultCode.INVALID_TP_PRICE);
    }

    @DisplayName("단계별 이벤트: 각 결과 코드와 성공 여부로 발행한다")
    @Test
    void whenPublishingStages_thenSendsResultCodes() {
        OtocoDto dto = new OtocoDto(1L, "user", "005930", OtocoEntryDirection.BELOW, LeverageRatio.SPOT, 10,
                70_000, 73_500, 67_900, 100L, OtocoStatus.WAITING_EXIT, ORDER_TIME, 4);

        sut.publishEntryTriggered(dto);
        sut.publishEntryPartiallyFilled(dto);
        sut.publishEntryFilled(dto);
        sut.publishEntryFailed(dto, OtocoResultCode.ENTRY_FAILED);
        sut.publishEntryCanceled(dto);
        sut.publishExitTriggered(dto, OtocoLeg.TAKE_PROFIT);
        sut.publishExitTriggered(dto, OtocoLeg.STOP_LOSS);
        sut.publishExitFailed(dto, OtocoResultCode.INTERNAL_ERROR);

        List<OtocoResponseEvent> events = captured();
        assertThat(events).extracting(OtocoResponseEvent::resultCode, OtocoResponseEvent::success).containsExactly(
                tuple(OtocoResultCode.ENTRY_TRIGGERED, true),
                tuple(OtocoResultCode.ENTRY_PARTIALLY_FILLED, true),
                tuple(OtocoResultCode.ENTRY_FILLED, true),
                tuple(OtocoResultCode.ENTRY_FAILED, false),
                tuple(OtocoResultCode.ENTRY_CANCELED, true),
                tuple(OtocoResultCode.TP_TRIGGERED, true),
                tuple(OtocoResultCode.SL_TRIGGERED, true),
                tuple(OtocoResultCode.INTERNAL_ERROR, false));
        assertThat(events.get(1).entryRemainingQuantity()).isEqualTo(4);
        assertThat(events.get(0).orderTime()).isEqualTo(ORDER_TIME);
    }

    @DisplayName("Redis 발행이 실패해도 예외를 전파하지 않는다")
    @Test
    void givenRedisFails_whenPublishing_thenSwallows() {
        given(redisTemplate.convertAndSend(anyString(), any(OtocoResponseEvent.class)))
                .willThrow(new IllegalStateException("redis down"));
        OtocoDto dto = new OtocoDto(1L, "user", "005930", OtocoEntryDirection.BELOW, LeverageRatio.SPOT, 10,
                70_000, 73_500, 67_900, null, OtocoStatus.WAITING_ENTRY, ORDER_TIME, null);

        assertThatNoException().isThrownBy(() -> {
            sut.publishEntryFilled(dto);
            sut.publish(StockServerOtocoResponseMessage.fromEntity(
                    arile.toy.stocksystem.stockserver.otoco.OtocoFixtures.entity(OtocoStatus.WAITING_ENTRY)));
            sut.publishError(new StockServerOtocoRequestEvent("user", "005930", OtocoEntryDirection.BELOW, 10, 70_000,
                            OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 67_900, null, null),
                    OtocoResultCode.INSUFFICIENT_BALANCE);
        });
    }

    private List<OtocoResponseEvent> captured() {
        ArgumentCaptor<OtocoResponseEvent> captor = ArgumentCaptor.forClass(OtocoResponseEvent.class);
        then(redisTemplate).should(atLeastOnce()).convertAndSend(eq(CHANNEL), captor.capture());
        return captor.getAllValues();
    }
}

package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.TrailingStopLockRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.trailingstop.dto.*;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.event.publisher.TrailingStopResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trailingstop.registry.TrailingStopBookRegistry;
import arile.toy.stocksystem.stockserver.trailingstop.repository.StockServerTrailingStopResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 트레일링 스탑 추적·발동 테스트")
@ExtendWith(MockitoExtension.class)
class TrailingStopTriggerServiceTest {

    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";

    @InjectMocks private TrailingStopTriggerService sut;

    @Mock private TrailingStopBookRegistry trailingStopBookRegistry;
    @Mock private TrailingStopLockRegistry trailingStopLockRegistry;
    @Mock private TrailingStopService trailingStopService;
    @Mock private StockServerTrailingStopResponseRepository stockServerTrailingStopResponseRepository;
    @Mock private OrderService orderService;
    @Mock private TrailingStopResponseEventPublisher trailingStopResponseEventPublisher;
    @Mock private AccountApiClient accountApiClient;
    @Mock private TrailingStopTrailPersister trailingStopTrailPersister;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @BeforeEach
    void setUp() {
        given(trailingStopLockRegistry.lock(STOCK_CODE)).willReturn(new ReentrantLock());
    }

    @Nested
    @DisplayName("추적")
    class Trail {

        @DisplayName("매도: 기준가보다 높은 가격이면 기준가·발동가를 올리고 북 갱신·저장 표시·응답 갱신·발행한다")
        @Test
        void givenSellNewHigh_whenTick_thenTrailsUp() {
            givenBook(dto(TrailingStopType.SELL, 70_000, 67_900, 67_900));

            sut.getExternalTickMessageAndTrail(tick(72_000));

            ArgumentCaptor<TrailingStopDto> captor = ArgumentCaptor.forClass(TrailingStopDto.class);
            then(trailingStopBookRegistry).should().update(captor.capture());
            TrailingStopDto updated = captor.getValue();
            assertThat(updated.basePrice()).isEqualTo(72_000);
            assertThat(updated.triggerPrice()).isEqualTo(69_800);
            assertThat(updated.initialTriggerPrice()).isEqualTo(67_900);

            then(trailingStopTrailPersister).should().markDirty(updated);
            then(stockServerTrailingStopResponseRepository).should().update(eq(USERNAME), eq(1L), any());
            then(trailingStopResponseEventPublisher).should().publishTrailingUpdate(updated);
            then(trailingStopService).shouldHaveNoInteractions();
        }

        @DisplayName("매수: 기준가보다 낮은 가격이면 기준가·발동가를 내린다")
        @Test
        void givenBuyNewLow_whenTick_thenTrailsDown() {
            givenBook(dto(TrailingStopType.BUY, 70_000, 72_100, 72_100));

            sut.getExternalTickMessageAndTrail(tick(68_000));

            then(trailingStopBookRegistry).should().update(argThat(d ->
                    d.basePrice() == 68_000 && d.triggerPrice() == 70_100 && d.initialTriggerPrice() == 72_100));
            then(trailingStopTrailPersister).should().markDirty(any());
        }

        @DisplayName("발동가와 기준가 사이의 가격이면 아무것도 하지 않는다")
        @Test
        void givenPriceBetween_whenTick_thenNothing() {
            givenBook(dto(TrailingStopType.SELL, 70_000, 67_900, 67_900));

            sut.getExternalTickMessageAndTrail(tick(69_000));

            then(trailingStopBookRegistry).should(never()).update(any());
            then(trailingStopTrailPersister).shouldHaveNoInteractions();
            then(trailingStopResponseEventPublisher).shouldHaveNoInteractions();
        }

        @DisplayName("응답 갱신이 실패해도 발행하고 같은 틱의 다음 트레일링 스탑을 계속 처리한다")
        @Test
        void givenResponseUpdateFails_whenTick_thenContinues() {
            TrailingStopDto first = dto(1L, TrailingStopType.SELL, 70_000, 67_900, 67_900);
            TrailingStopDto second = dto(2L, TrailingStopType.SELL, 70_000, 67_900, 67_900);
            givenBook(first, second);
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerTrailingStopResponseRepository).update(eq(USERNAME), eq(1L), any());

            sut.getExternalTickMessageAndTrail(tick(72_000));

            then(trailingStopBookRegistry).should(times(2)).update(any());
            then(trailingStopTrailPersister).should(times(2)).markDirty(any());
            then(trailingStopResponseEventPublisher).should(times(2)).publishTrailingUpdate(any());
        }
    }

    @Nested
    @DisplayName("발동")
    class Trigger {

        @DisplayName("매수 발동: 초기 발동가 기준 예약액과 현재 발동가 기준 주문액의 차액을 환불하고 주문을 등록한다")
        @Test
        void givenBuyTriggered_whenTick_thenRefundsDifferenceAndOrders() {
            // 예약 721,108 (72,100×10 + 108) / 주문 701,105 (70,100×10 + 105) → 차액 20,003
            TrailingStopDto dto = dto(TrailingStopType.BUY, 68_000, 70_100, 72_100);
            givenBook(dto);
            givenTriggerResult(TrailingStopStatus.ACTIVE);
            given(accountApiClient.refundReservedCash(USERNAME, 20_003L)).willReturn(true);

            sut.getExternalTickMessageAndTrail(tick(70_100));

            then(trailingStopBookRegistry).should().remove(STOCK_CODE, 1L);
            then(accountApiClient).should().refundReservedCash(USERNAME, 20_003L);
            then(orderService).should().registerOrder(any(), eq(true));
            then(stockServerTrailingStopResponseRepository).should().delete(USERNAME, 1L);
            then(trailingStopResponseEventPublisher).should().publishTrigger(USERNAME);
        }

        @DisplayName("매수 발동: 추적이 없어 차액이 0이면 환불하지 않는다")
        @Test
        void givenNoDifference_whenTriggered_thenNoRefund() {
            givenBook(dto(TrailingStopType.BUY, 70_000, 72_100, 72_100));
            givenTriggerResult(TrailingStopStatus.ACTIVE);

            sut.getExternalTickMessageAndTrail(tick(72_100));

            then(accountApiClient).should(never()).refundReservedCash(anyString(), anyLong());
            then(orderService).should().registerOrder(any(), eq(true));
        }

        @DisplayName("차액 환불이 실패해도 주문은 등록한다")
        @Test
        void givenDifferenceRefundFails_whenTriggered_thenStillOrders() {
            givenBook(dto(TrailingStopType.BUY, 68_000, 70_100, 72_100));
            givenTriggerResult(TrailingStopStatus.ACTIVE);
            given(accountApiClient.refundReservedCash(USERNAME, 20_003L)).willReturn(false);

            sut.getExternalTickMessageAndTrail(tick(70_100));

            then(orderService).should().registerOrder(any(), eq(true));
        }

        @DisplayName("매도 발동: 환불 없이 주문을 등록한다")
        @Test
        void givenSellTriggered_whenTick_thenOrders() {
            givenBook(dto(TrailingStopType.SELL, 72_000, 69_800, 67_900));
            givenTriggerResult(TrailingStopStatus.ACTIVE);

            sut.getExternalTickMessageAndTrail(tick(69_800));

            then(accountApiClient).shouldHaveNoInteractions();
            then(orderService).should().registerOrder(any(), eq(true));
        }

        @DisplayName("이미 취소·발동된 트레일링 스탑이면 주문하지 않는다")
        @Test
        void givenAlreadyClosed_whenTriggered_thenSkips() {
            givenBook(dto(TrailingStopType.SELL, 70_000, 67_900, 67_900));
            givenTriggerResult(TrailingStopStatus.CANCELED);

            sut.getExternalTickMessageAndTrail(tick(67_000));

            then(orderService).shouldHaveNoInteractions();
            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("발동 상태 변경이 실패하면 북에 다시 등록하고 주문하지 않는다")
        @Test
        void givenStatusUpdateFails_whenTriggered_thenReRegisters() {
            TrailingStopDto dto = dto(TrailingStopType.SELL, 70_000, 67_900, 67_900);
            givenBook(dto);
            given(trailingStopService.updateTrailingStopStatusByTrigger(1L))
                    .willThrow(new IllegalStateException("db error"));

            sut.getExternalTickMessageAndTrail(tick(67_000));

            then(trailingStopBookRegistry).should().register(dto);
            then(orderService).shouldHaveNoInteractions();
        }

        @DisplayName("주문 등록 실패 시 매수는 현재 발동가 기준 주문액을 환불하고 실패를 발행한다")
        @Test
        void givenBuyOrderFails_whenTriggered_thenCompensates() {
            TrailingStopDto dto = dto(TrailingStopType.BUY, 68_000, 70_100, 72_100);
            givenBook(dto);
            givenTriggerResult(TrailingStopStatus.ACTIVE);
            given(accountApiClient.refundReservedCash(USERNAME, 20_003L)).willReturn(true);
            given(accountApiClient.refundReservedCash(USERNAME, 701_105L)).willReturn(true);
            willThrow(new IllegalStateException("order error")).given(orderService).registerOrder(any(), eq(true));

            sut.getExternalTickMessageAndTrail(tick(70_100));

            then(accountApiClient).should().refundReservedCash(USERNAME, 701_105L);
            then(stockServerTrailingStopResponseRepository).should().delete(USERNAME, 1L);
            then(trailingStopResponseEventPublisher).should().publishTriggerFailure(dto, TrailingStopResultCode.INTERNAL_ERROR);
            then(trailingStopResponseEventPublisher).should(never()).publishTrigger(anyString());
        }

        @DisplayName("주문 등록 실패 시 매도는 현물 주식 / 레버리지 포지션을 환불한다")
        @Test
        void givenSellOrderFails_whenTriggered_thenRefundsStock() {
            TrailingStopDto spot = dto(1L, TrailingStopType.SELL, LeverageRatio.SPOT, 70_000, 67_900, 67_900);
            TrailingStopDto leverage = dto(2L, TrailingStopType.SELL, LeverageRatio.X2, 70_000, 67_900, 67_900);
            givenBook(spot, leverage);
            given(trailingStopService.updateTrailingStopStatusByTrigger(anyLong()))
                    .willReturn(new UpdateTrailingStopStatusResult(null, TrailingStopStatus.ACTIVE));
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            given(accountApiClient.refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10)).willReturn(true);
            willThrow(new IllegalStateException("order error")).given(orderService).registerOrder(any(), eq(true));

            sut.getExternalTickMessageAndTrail(tick(67_000));

            then(accountApiClient).should().refundReservedStock(USERNAME, STOCK_CODE, 10);
            then(accountApiClient).should().refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10);
            then(trailingStopResponseEventPublisher).should(times(2)).publishTriggerFailure(any(), eq(TrailingStopResultCode.INTERNAL_ERROR));
        }

        @DisplayName("보상 중 응답 삭제가 실패해도 실패 이벤트는 발행한다")
        @Test
        void givenCompensationDeleteFails_whenTriggered_thenStillPublishesFailure() {
            TrailingStopDto dto = dto(TrailingStopType.SELL, 70_000, 67_900, 67_900);
            givenBook(dto);
            givenTriggerResult(TrailingStopStatus.ACTIVE);
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            willThrow(new IllegalStateException("order error")).given(orderService).registerOrder(any(), eq(true));
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerTrailingStopResponseRepository).delete(USERNAME, 1L);

            sut.getExternalTickMessageAndTrail(tick(67_000));

            then(trailingStopResponseEventPublisher).should().publishTriggerFailure(dto, TrailingStopResultCode.INTERNAL_ERROR);
        }

        @DisplayName("주문 등록 성공 후 응답 삭제가 실패해도 환불(보상)하지 않고 발동 이벤트를 발행한다")
        @Test
        void givenDeleteFailsAfterOrder_whenTriggered_thenNoCompensation() {
            givenBook(dto(TrailingStopType.SELL, 70_000, 67_900, 67_900));
            givenTriggerResult(TrailingStopStatus.ACTIVE);
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerTrailingStopResponseRepository).delete(USERNAME, 1L);

            sut.getExternalTickMessageAndTrail(tick(67_000));

            then(accountApiClient).shouldHaveNoInteractions();
            then(trailingStopResponseEventPublisher).should().publishTrigger(USERNAME);
            then(trailingStopResponseEventPublisher).should(never()).publishTriggerFailure(any(), any());
        }
    }

    // ===== helpers =====

    private TradePriceTickMessage tick(int price) {
        TradePriceTickMessage tick = mock(TradePriceTickMessage.class);
        given(tick.stockCode()).willReturn(STOCK_CODE);
        given(tick.curPrice()).willReturn(price);
        return tick;
    }

    private void givenBook(TrailingStopDto... dtos) {
        given(trailingStopBookRegistry.getAll(STOCK_CODE)).willReturn(List.of(dtos));
    }

    private void givenTriggerResult(TrailingStopStatus previous) {
        given(trailingStopService.updateTrailingStopStatusByTrigger(1L))
                .willReturn(new UpdateTrailingStopStatusResult(null, previous));
    }

    private TrailingStopDto dto(TrailingStopType type, int base, int trigger, int initialTrigger) {
        return dto(1L, type, base, trigger, initialTrigger);
    }

    private TrailingStopDto dto(Long id, TrailingStopType type, int base, int trigger, int initialTrigger) {
        return dto(id, type, LeverageRatio.SPOT, base, trigger, initialTrigger);
    }

    private TrailingStopDto dto(Long id, TrailingStopType type, LeverageRatio ratio,
                                int base, int trigger, int initialTrigger) {
        return new TrailingStopDto(id, USERNAME, STOCK_CODE, type, ratio, 10, 3.0,
                base, trigger, initialTrigger, TrailingStopStatus.ACTIVE, Instant.now());
    }
}

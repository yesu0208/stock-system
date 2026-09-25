package arile.toy.stocksystem.stockserver.autoorder.service;

import arile.toy.stocksystem.stockserver.autoorder.dto.*;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.AutoOrderRepository;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 자동주문 등록·상태 변경 테스트")
@ExtendWith(MockitoExtension.class)
class AutoOrderServiceTest {

    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";
    // 주문가 70,000 × 10주 = 700,000 → 수수료 105
    private static final long SPOT_RESERVE = 700_105L;

    @InjectMocks private AutoOrderService sut;

    @Mock private AutoOrderRepository autoOrderRepository;
    @Mock private AutoOrderQueueRegistry autoOrderQueueRegistry;
    @Mock private AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;
    @Mock private StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @Nested
    @DisplayName("registerAutoOrder")
    class Register {

        @DisplayName("현물 매수: 주문금액+수수료를 예약하고 ACTIVE로 저장·대기열 등록·응답 발행한다")
        @Test
        void givenSpotBuy_whenRegistering_thenReservesAndSaves() {
            var request = request(AutoOrderType.BUY, LeverageRatio.SPOT);
            given(accountApiClient.reserveCash(USERNAME, SPOT_RESERVE)).willReturn(true);
            givenSaveAssignsId();

            sut.registerAutoOrder(request);

            ArgumentCaptor<AutoOrderEntity> captor = ArgumentCaptor.forClass(AutoOrderEntity.class);
            then(autoOrderRepository).should().save(captor.capture());
            AutoOrderEntity saved = captor.getValue();
            assertThat(saved.getAutoOrderStatus()).isEqualTo(AutoOrderStatus.ACTIVE);
            assertThat(saved.getTriggerPrice()).isEqualTo(71_000);
            assertThat(saved.getOrderPrice()).isEqualTo(70_000);

            then(autoOrderQueueRegistry).should().autoOrderEnqueue(argThat(d -> d.autoOrderId().equals(1L)));
            then(stockServerAutoOrderResponseRepository).should().save(any(StockServerAutoOrderResponseMessage.class));
            then(autoOrderResponseEventPublisher).should().publish(any(StockServerAutoOrderResponseMessage.class));
        }

        @DisplayName("레버리지 매수: 증거금+수수료만 예약한다")
        @Test
        void givenLeverageBuy_whenRegistering_thenReservesMarginPlusFee() {
            var request = request(AutoOrderType.BUY, LeverageRatio.X2);
            given(accountApiClient.reserveCash(USERNAME, 350_105L)).willReturn(true);
            givenSaveAssignsId();

            sut.registerAutoOrder(request);

            then(accountApiClient).should().reserveCash(USERNAME, 350_105L);
        }

        @DisplayName("레버리지 비율이 없으면 현물로 저장한다")
        @Test
        void givenNullLeverage_whenRegistering_thenSavesAsSpot() {
            var request = request(AutoOrderType.BUY, null);
            given(accountApiClient.reserveCash(USERNAME, SPOT_RESERVE)).willReturn(true);
            givenSaveAssignsId();

            sut.registerAutoOrder(request);

            then(autoOrderRepository).should().save(argThat(e -> e.getLeverageRatio() == LeverageRatio.SPOT));
        }

        @DisplayName("현금 예약에 실패하면 잔고 부족 에러를 발행하고 저장하지 않는다")
        @Test
        void givenReserveCashFails_whenRegistering_thenPublishesInsufficientBalance() {
            var request = request(AutoOrderType.BUY, LeverageRatio.SPOT);
            given(accountApiClient.reserveCash(USERNAME, SPOT_RESERVE)).willReturn(false);

            sut.registerAutoOrder(request);

            then(autoOrderResponseEventPublisher).should().publishError(request, AutoOrderResultCode.INSUFFICIENT_BALANCE);
            then(autoOrderRepository).shouldHaveNoInteractions();
        }

        @DisplayName("현물 매도는 주식을, 레버리지 매도는 레버리지 포지션을 예약한다")
        @Test
        void givenSell_whenRegistering_thenReservesStockOrLeverage() {
            given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            given(accountApiClient.reserveLeverageStock(USERNAME, STOCK_CODE, "X2", 10)).willReturn(true);
            givenSaveAssignsId();

            sut.registerAutoOrder(request(AutoOrderType.SELL, LeverageRatio.SPOT));
            sut.registerAutoOrder(request(AutoOrderType.SELL, LeverageRatio.X2));

            then(accountApiClient).should().reserveStock(USERNAME, STOCK_CODE, 10);
            then(accountApiClient).should().reserveLeverageStock(USERNAME, STOCK_CODE, "X2", 10);
            then(accountApiClient).should(never()).reserveCash(anyString(), anyLong());
        }

        @DisplayName("주식 예약에 실패하면 보유 주식 부족 에러를 발행하고 저장하지 않는다")
        @Test
        void givenReserveStockFails_whenRegistering_thenPublishesInsufficientStock() {
            var request = request(AutoOrderType.SELL, LeverageRatio.SPOT);
            given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, 10)).willReturn(false);

            sut.registerAutoOrder(request);

            then(autoOrderResponseEventPublisher).should().publishError(request, AutoOrderResultCode.INSUFFICIENT_STOCK);
            then(autoOrderRepository).shouldHaveNoInteractions();
        }

        @DisplayName("저장 후 대기열 등록이 실패하면 CANCELED로 무효화하고 환불·에러 발행 후 예외를 다시 던진다")
        @Test
        void givenEnqueueFails_whenRegistering_thenCancelsRefundsAndRethrows() {
            var request = request(AutoOrderType.BUY, LeverageRatio.SPOT);
            given(accountApiClient.reserveCash(USERNAME, SPOT_RESERVE)).willReturn(true);
            givenSaveAssignsId();
            willThrow(new IllegalStateException("queue error")).given(autoOrderQueueRegistry).autoOrderEnqueue(any());

            assertThatThrownBy(() -> sut.registerAutoOrder(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("queue error");

            ArgumentCaptor<AutoOrderEntity> captor = ArgumentCaptor.forClass(AutoOrderEntity.class);
            then(autoOrderRepository).should(times(2)).save(captor.capture());
            assertThat(captor.getValue().getAutoOrderStatus()).isEqualTo(AutoOrderStatus.CANCELED);

            then(autoOrderQueueRegistry).should().autoOrderCancel(1L, STOCK_CODE);
            then(accountApiClient).should().refundReservedCash(USERNAME, SPOT_RESERVE);
            then(autoOrderResponseEventPublisher).should().publishError(request, AutoOrderResultCode.INTERNAL_ERROR);
            then(stockServerAutoOrderResponseRepository).shouldHaveNoInteractions();
        }

        @DisplayName("저장 자체가 실패하면 취소 처리 없이 예약분만 환불한다 (레버리지 매도 → 레버리지 포지션 환불)")
        @Test
        void givenSaveFails_whenRegistering_thenRefundsOnly() {
            var request = request(AutoOrderType.SELL, LeverageRatio.X2);
            given(accountApiClient.reserveLeverageStock(USERNAME, STOCK_CODE, "X2", 10)).willReturn(true);
            given(autoOrderRepository.save(any(AutoOrderEntity.class))).willThrow(new IllegalStateException("db error"));

            assertThatThrownBy(() -> sut.registerAutoOrder(request)).isInstanceOf(IllegalStateException.class);

            then(autoOrderRepository).should(times(1)).save(any());
            then(autoOrderQueueRegistry).shouldHaveNoInteractions();
            then(accountApiClient).should().refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10);
        }

        @DisplayName("저장 실패 시 현물 매도는 보유 주식을 환불한다")
        @Test
        void givenSpotSellSaveFails_whenRegistering_thenRefundsStock() {
            var request = request(AutoOrderType.SELL, LeverageRatio.SPOT);
            given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            given(autoOrderRepository.save(any(AutoOrderEntity.class))).willThrow(new IllegalStateException("db error"));

            assertThatThrownBy(() -> sut.registerAutoOrder(request)).isInstanceOf(IllegalStateException.class);

            then(accountApiClient).should().refundReservedStock(USERNAME, STOCK_CODE, 10);
        }

        @DisplayName("등록 후 Redis 응답 저장이 실패해도 예외 없이 응답을 발행한다 (재시도로 인한 중복 등록 방지)")
        @Test
        void givenResponseSaveFails_whenRegistering_thenDoesNotThrow() {
            var request = request(AutoOrderType.BUY, LeverageRatio.SPOT);
            given(accountApiClient.reserveCash(USERNAME, SPOT_RESERVE)).willReturn(true);
            givenSaveAssignsId();
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerAutoOrderResponseRepository).save(any());

            sut.registerAutoOrder(request);

            then(autoOrderResponseEventPublisher).should().publish(any(StockServerAutoOrderResponseMessage.class));
            then(accountApiClient).should(never()).refundReservedCash(anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class StatusChange {

        @DisplayName("사용자 취소: 소유자·종목이 일치하는 ACTIVE 자동주문은 CANCELED로 바뀐다")
        @Test
        void givenOwner_whenUserCancel_thenCanceled() {
            AutoOrderEntity entity = givenEntity(AutoOrderStatus.ACTIVE);

            var result = sut.updateAutoOrderStatusByUserCancel(1L, USERNAME, STOCK_CODE);

            assertThat(result).get().extracting(UpdateAutoOrderStatusResult::previousStatus)
                    .isEqualTo(AutoOrderStatus.ACTIVE);
            assertThat(entity.getAutoOrderStatus()).isEqualTo(AutoOrderStatus.CANCELED);
        }

        @DisplayName("사용자 취소: 소유자가 아니거나, 종목이 다르거나, 요청자가 null이면 empty이고 상태를 바꾸지 않는다")
        @Test
        void givenNotOwner_whenUserCancel_thenEmpty() {
            AutoOrderEntity entity = givenEntity(AutoOrderStatus.ACTIVE);

            assertThat(sut.updateAutoOrderStatusByUserCancel(1L, "other", STOCK_CODE)).isEmpty();
            assertThat(sut.updateAutoOrderStatusByUserCancel(1L, USERNAME, "000660")).isEmpty();
            assertThat(sut.updateAutoOrderStatusByUserCancel(1L, null, STOCK_CODE)).isEmpty();
            assertThat(entity.getAutoOrderStatus()).isEqualTo(AutoOrderStatus.ACTIVE);
        }

        @DisplayName("취소: 이미 취소·발동된 자동주문은 상태를 바꾸지 않고 이전 상태를 돌려준다")
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = AutoOrderStatus.class, names = {"CANCELED", "TRIGGERED"})
        void givenClosed_whenCancel_thenUnchanged(AutoOrderStatus status) {
            AutoOrderEntity entity = givenEntity(status);

            var result = sut.updateAutoOrderStatusByCancel(1L);

            assertThat(result.previousStatus()).isEqualTo(status);
            assertThat(entity.getAutoOrderStatus()).isEqualTo(status);
        }

        @DisplayName("자동주문이 없으면 예외를 던진다")
        @Test
        void givenNotFound_whenChangingStatus_thenThrows() {
            given(autoOrderRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateAutoOrderStatusByCancel(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("auto order not found");
        }

        @DisplayName("발동: ACTIVE 자동주문만 TRIGGERED로 바꾼다")
        @Test
        void givenActive_whenTriggering_thenTriggered() {
            AutoOrderEntity entity = givenEntity(AutoOrderStatus.ACTIVE);

            var result = sut.updateAutoOrderStatusByTrigger(1L);

            assertThat(result.previousStatus()).isEqualTo(AutoOrderStatus.ACTIVE);
            assertThat(entity.getAutoOrderStatus()).isEqualTo(AutoOrderStatus.TRIGGERED);
        }

        @DisplayName("발동: ACTIVE가 아니면 상태를 바꾸지 않는다 (취소와 발동이 겹친 경우)")
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = AutoOrderStatus.class, names = {"CANCELED", "TRIGGERED"})
        void givenNotActive_whenTriggering_thenUnchanged(AutoOrderStatus status) {
            AutoOrderEntity entity = givenEntity(status);

            var result = sut.updateAutoOrderStatusByTrigger(1L);

            assertThat(result.previousStatus()).isEqualTo(status);
            assertThat(entity.getAutoOrderStatus()).isEqualTo(status);
        }

        @DisplayName("미발동 자동주문 조회를 저장소에 위임한다")
        @Test
        void whenFindingUntriggered_thenDelegates() {
            List<AutoOrderEntity> entities = List.of(new AutoOrderEntity());
            given(autoOrderRepository.findAllUntriggered(List.of(STOCK_CODE))).willReturn(entities);

            assertThat(sut.findAllUntriggeredAutoOrders(List.of(STOCK_CODE))).isSameAs(entities);
        }
    }

    // ===== helpers =====

    private StockServerAutoOrderRequestEvent request(AutoOrderType type, LeverageRatio ratio) {
        return StockServerAutoOrderRequestEvent.of(USERNAME, STOCK_CODE, type, 71_000, 70_000, 10, ratio);
    }

    private void givenSaveAssignsId() {
        given(autoOrderRepository.save(any(AutoOrderEntity.class))).willAnswer(invocation -> {
            AutoOrderEntity entity = invocation.getArgument(0);
            entity.setAutoOrderId(1L);
            return entity;
        });
    }

    private AutoOrderEntity givenEntity(AutoOrderStatus status) {
        AutoOrderEntity entity = AutoOrderEntity.of(USERNAME, STOCK_CODE, AutoOrderType.BUY, LeverageRatio.SPOT,
                71_000, 70_000, 10, status);
        entity.setAutoOrderId(1L);
        given(autoOrderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
        return entity;
    }
}

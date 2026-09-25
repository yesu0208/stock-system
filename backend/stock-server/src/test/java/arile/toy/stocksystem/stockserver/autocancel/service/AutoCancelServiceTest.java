package arile.toy.stocksystem.stockserver.autocancel.service;

import arile.toy.stocksystem.stockserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.autocancel.entity.AutoCancelEntity;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.autocancel.event.publisher.AutoCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autocancel.repository.AutoCancelRepository;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.UpdateAutoOrderStatusResult;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderService;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 자동주문 취소 테스트")
@ExtendWith(MockitoExtension.class)
class AutoCancelServiceTest {

    private static final Long AUTO_ORDER_ID = 1L;
    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";
    // 주문가 70,000 × 10 = 700,000 → 현물 예약금 700,105 / X2 예약금 350,105
    private static final long SPOT_RESERVE = 700_105L;

    @InjectMocks private AutoCancelService sut;

    @Mock private AutoOrderService autoOrderService;
    @Mock private AutoCancelRepository autoCancelRepository;
    @Mock private AutoOrderQueueRegistry autoOrderQueueRegistry;
    @Mock private AutoCancelResponseEventPublisher autoCancelResponseEventPublisher;
    @Mock private StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @Nested
    @DisplayName("사용자 취소 (registerAutoCancel)")
    class RegisterAutoCancel {

        @DisplayName("소유자·종목 검증에 실패하면 아무 처리도, 응답도 하지 않는다")
        @Test
        void givenNotOwner_whenCancelling_thenDoesNothing() {
            given(autoOrderService.updateAutoOrderStatusByUserCancel(AUTO_ORDER_ID, "other", STOCK_CODE))
                    .willReturn(Optional.empty());

            sut.registerAutoCancel(AutoCancelRequestEvent.of(AUTO_ORDER_ID, STOCK_CODE, "other"));

            then(accountApiClient).shouldHaveNoInteractions();
            then(autoCancelRepository).shouldHaveNoInteractions();
            then(autoCancelResponseEventPublisher).shouldHaveNoInteractions();
        }

        @DisplayName("이미 취소된 자동주문이면 ALREADY_CANCELLED 실패 응답만 보낸다")
        @Test
        void givenAlreadyCanceled_whenCancelling_thenPublishesAlreadyCancelled() {
            givenUserCancel(entity(AutoOrderType.BUY, LeverageRatio.SPOT), AutoOrderStatus.CANCELED);

            sut.registerAutoCancel(request());

            then(autoCancelResponseEventPublisher).should().publish(argThat(e ->
                    !e.success() && e.errorCode() == AutoCancelErrorCode.ALREADY_CANCELLED));
            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("이미 발동된 자동주문이면 ALREADY_TRIGGERED 실패 응답만 보낸다")
        @Test
        void givenAlreadyTriggered_whenCancelling_thenPublishesAlreadyTriggered() {
            givenUserCancel(entity(AutoOrderType.BUY, LeverageRatio.SPOT), AutoOrderStatus.TRIGGERED);

            sut.registerAutoCancel(request());

            then(autoCancelResponseEventPublisher).should().publish(argThat(e ->
                    !e.success() && e.errorCode() == AutoCancelErrorCode.ALREADY_TRIGGERED));
            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("현물 매수: 예약금(주문금액+수수료)을 환불하고 취소 이력·대기열 제거·응답 삭제·성공 응답을 처리한다")
        @Test
        void givenSpotBuy_whenCancelling_thenRefundsAndCompletes() {
            givenUserCancel(entity(AutoOrderType.BUY, LeverageRatio.SPOT), AutoOrderStatus.ACTIVE);
            given(accountApiClient.refundReservedCash(USERNAME, SPOT_RESERVE)).willReturn(true);

            sut.registerAutoCancel(request());

            then(autoCancelRepository).should().save(argThat((AutoCancelEntity c) -> c.getAutoOrderId().equals(AUTO_ORDER_ID)));
            then(autoOrderQueueRegistry).should().autoOrderCancel(AUTO_ORDER_ID, STOCK_CODE);
            then(stockServerAutoOrderResponseRepository).should().delete(USERNAME, AUTO_ORDER_ID);
            then(autoCancelResponseEventPublisher).should().publish(argThat(AutoCancelResponseEvent::success));
        }

        @DisplayName("레버리지 매수는 증거금+수수료를, 현물·레버리지 매도는 주식·포지션을 환불한다")
        @Test
        void givenOtherTypes_whenCancelling_thenRefundsByType() {
            given(accountApiClient.refundReservedCash(USERNAME, 350_105L)).willReturn(true);
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            given(accountApiClient.refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10)).willReturn(true);

            givenUserCancel(entity(AutoOrderType.BUY, LeverageRatio.X2), AutoOrderStatus.ACTIVE);
            sut.registerAutoCancel(request());
            givenUserCancel(entity(AutoOrderType.SELL, LeverageRatio.SPOT), AutoOrderStatus.ACTIVE);
            sut.registerAutoCancel(request());
            givenUserCancel(entity(AutoOrderType.SELL, LeverageRatio.X2), AutoOrderStatus.ACTIVE);
            sut.registerAutoCancel(request());

            then(accountApiClient).should().refundReservedCash(USERNAME, 350_105L);
            then(accountApiClient).should().refundReservedStock(USERNAME, STOCK_CODE, 10);
            then(accountApiClient).should().refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10);
        }

        @DisplayName("취소 이력 저장(DB) 후 환불하고, 환불 성공 후에 대기열에서 제거한다")
        @Test
        void givenActive_whenCancelling_thenOrdersDbRefundQueue() {
            givenUserCancel(entity(AutoOrderType.BUY, LeverageRatio.SPOT), AutoOrderStatus.ACTIVE);
            given(accountApiClient.refundReservedCash(USERNAME, SPOT_RESERVE)).willReturn(true);

            sut.registerAutoCancel(request());

            InOrder inOrder = inOrder(autoCancelRepository, accountApiClient, autoOrderQueueRegistry);
            inOrder.verify(autoCancelRepository).save(any(AutoCancelEntity.class));
            inOrder.verify(accountApiClient).refundReservedCash(USERNAME, SPOT_RESERVE);
            inOrder.verify(autoOrderQueueRegistry).autoOrderCancel(AUTO_ORDER_ID, STOCK_CODE);
        }

        @DisplayName("환불에 실패하면 내부 에러 응답 후 예외를 다시 던지고(롤백), 대기열에서 제거하지 않는다")
        @Test
        void givenRefundFails_whenCancelling_thenPublishesErrorAndRethrows() {
            givenUserCancel(entity(AutoOrderType.BUY, LeverageRatio.SPOT), AutoOrderStatus.ACTIVE);
            given(accountApiClient.refundReservedCash(USERNAME, SPOT_RESERVE)).willReturn(false);

            assertThatThrownBy(() -> sut.registerAutoCancel(request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cash refund failed");

            then(autoOrderQueueRegistry).shouldHaveNoInteractions();
            then(stockServerAutoOrderResponseRepository).shouldHaveNoInteractions();
            then(autoCancelResponseEventPublisher).should().publish(argThat(e ->
                    !e.success() && e.errorCode() == AutoCancelErrorCode.INTERNAL_ERROR));
        }

        @DisplayName("주식 환불에 실패해도 예외를 다시 던진다")
        @Test
        void givenStockRefundFails_whenCancelling_thenRethrows() {
            givenUserCancel(entity(AutoOrderType.SELL, LeverageRatio.SPOT), AutoOrderStatus.ACTIVE);
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(false);

            assertThatThrownBy(() -> sut.registerAutoCancel(request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Stock refund failed");
        }

        @DisplayName("취소 이력 저장이 실패하면 환불하지 않고 예외를 다시 던진다")
        @Test
        void givenCancelSaveFails_whenCancelling_thenDoesNotRefund() {
            givenUserCancel(entity(AutoOrderType.BUY, LeverageRatio.SPOT), AutoOrderStatus.ACTIVE);
            given(autoCancelRepository.save(any(AutoCancelEntity.class))).willThrow(new IllegalStateException("db error"));

            assertThatThrownBy(() -> sut.registerAutoCancel(request())).isInstanceOf(IllegalStateException.class);

            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("환불 이후 Redis 응답 삭제가 실패해도 예외 없이 성공 응답을 보낸다 (중복 환불 방지)")
        @Test
        void givenDeleteFailsAfterRefund_whenCancelling_thenStillSucceeds() {
            givenUserCancel(entity(AutoOrderType.BUY, LeverageRatio.SPOT), AutoOrderStatus.ACTIVE);
            given(accountApiClient.refundReservedCash(USERNAME, SPOT_RESERVE)).willReturn(true);
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerAutoOrderResponseRepository).delete(USERNAME, AUTO_ORDER_ID);

            assertThatCode(() -> sut.registerAutoCancel(request())).doesNotThrowAnyException();

            then(autoCancelResponseEventPublisher).should().publish(argThat(AutoCancelResponseEvent::success));
        }
    }

    @Nested
    @DisplayName("시스템 강제 취소 (forceAutoCancel)")
    class ForceAutoCancel {

        @DisplayName("이전 상태가 열린 상태(ACTIVE)가 아니면 아무 처리도 하지 않는다")
        @Test
        void givenNotOpen_whenForceCancelling_thenDoesNothing() {
            given(autoOrderService.updateAutoOrderStatusByCancel(AUTO_ORDER_ID))
                    .willReturn(UpdateAutoOrderStatusResult.of(entity(AutoOrderType.BUY, LeverageRatio.SPOT),
                            AutoOrderStatus.TRIGGERED));

            sut.forceAutoCancel(AUTO_ORDER_ID);

            then(accountApiClient).shouldHaveNoInteractions();
            then(autoCancelRepository).shouldHaveNoInteractions();
        }

        @DisplayName("열린 자동주문은 소유자 검증 없이 환불·취소 후 성공 응답을 보낸다")
        @Test
        void givenOpen_whenForceCancelling_thenCancels() {
            given(autoOrderService.updateAutoOrderStatusByCancel(AUTO_ORDER_ID))
                    .willReturn(UpdateAutoOrderStatusResult.of(entity(AutoOrderType.BUY, LeverageRatio.SPOT),
                            AutoOrderStatus.ACTIVE));
            given(accountApiClient.refundReservedCash(USERNAME, SPOT_RESERVE)).willReturn(true);

            sut.forceAutoCancel(AUTO_ORDER_ID);

            then(autoOrderQueueRegistry).should().autoOrderCancel(AUTO_ORDER_ID, STOCK_CODE);
            then(autoCancelResponseEventPublisher).should().publish(argThat(AutoCancelResponseEvent::success));
        }

        @DisplayName("환불에 실패해도 예외를 밖으로 던지지 않는다 (장 마감 정산이 예약금을 해제)")
        @Test
        void givenRefundFails_whenForceCancelling_thenSwallows() {
            given(autoOrderService.updateAutoOrderStatusByCancel(AUTO_ORDER_ID))
                    .willReturn(UpdateAutoOrderStatusResult.of(entity(AutoOrderType.BUY, LeverageRatio.SPOT),
                            AutoOrderStatus.ACTIVE));
            given(accountApiClient.refundReservedCash(USERNAME, SPOT_RESERVE)).willReturn(false);

            assertThatCode(() -> sut.forceAutoCancel(AUTO_ORDER_ID)).doesNotThrowAnyException();

            then(autoOrderQueueRegistry).shouldHaveNoInteractions();
            then(autoCancelResponseEventPublisher).shouldHaveNoInteractions();
        }
    }

    // ===== helpers =====

    private AutoCancelRequestEvent request() {
        return AutoCancelRequestEvent.of(AUTO_ORDER_ID, STOCK_CODE, USERNAME);
    }

    private void givenUserCancel(AutoOrderEntity entity, AutoOrderStatus previousStatus) {
        given(autoOrderService.updateAutoOrderStatusByUserCancel(AUTO_ORDER_ID, USERNAME, STOCK_CODE))
                .willReturn(Optional.of(UpdateAutoOrderStatusResult.of(entity, previousStatus)));
    }

    private AutoOrderEntity entity(AutoOrderType type, LeverageRatio ratio) {
        AutoOrderEntity entity = AutoOrderEntity.of(USERNAME, STOCK_CODE, type, ratio,
                71_000, 70_000, 10, AutoOrderStatus.CANCELED);
        entity.setAutoOrderId(AUTO_ORDER_ID);
        return entity;
    }
}

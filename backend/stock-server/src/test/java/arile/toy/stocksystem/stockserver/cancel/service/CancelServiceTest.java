package arile.toy.stocksystem.stockserver.cancel.service;

import arile.toy.stocksystem.stockserver.cancel.dto.CancelErrorCode;
import arile.toy.stocksystem.stockserver.cancel.entity.CancelEntity;
import arile.toy.stocksystem.stockserver.cancel.event.CancelRequestEvent;
import arile.toy.stocksystem.stockserver.cancel.event.CancelResponseEvent;
import arile.toy.stocksystem.stockserver.cancel.event.publisher.CancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.cancel.repository.CancelRepository;
import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.QueuePositionBroadcastService;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoOrderLifecycleListener;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 주문 취소 테스트")
@ExtendWith(MockitoExtension.class)
class CancelServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";

    @InjectMocks private CancelService sut;

    @Mock private OrderService orderService;
    @Mock private CancelRepository cancelRepository;
    @Mock private OrderQueueRegistry orderQueueRegistry;
    @Mock private CancelResponseEventPublisher cancelResponseEventPublisher;
    @Mock private StockServerOrderResponseRepository stockServerOrderResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Mock private OtocoOrderLifecycleListener otocoOrderLifecycleListener;
    @Mock private QueuePositionBroadcastService queuePositionBroadcastService;

    @Nested
    @DisplayName("사용자 취소 (registerCancel)")
    class RegisterCancel {

        @DisplayName("소유자·종목 검증에 실패하면 아무 처리도, 응답도 하지 않는다")
        @Test
        void givenNotOwner_whenCancelling_thenDoesNothing() {
            given(orderService.updateOrderStatusByUserCancel(ORDER_ID, "other", STOCK_CODE))
                    .willReturn(Optional.empty());

            sut.registerCancel(CancelRequestEvent.of(ORDER_ID, STOCK_CODE, "other"));

            then(accountApiClient).shouldHaveNoInteractions();
            then(cancelRepository).shouldHaveNoInteractions();
            then(orderQueueRegistry).shouldHaveNoInteractions();
            then(cancelResponseEventPublisher).shouldHaveNoInteractions();
        }

        @DisplayName("이미 취소된 주문이면 ALREADY_CANCELLED 실패 응답만 보낸다")
        @Test
        void givenAlreadyCanceled_whenCancelling_thenPublishesAlreadyCancelled() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.CANCELED);

            sut.registerCancel(request());

            then(cancelResponseEventPublisher).should().publish(argThat(e ->
                    !e.success() && e.errorCode() == CancelErrorCode.ALREADY_CANCELLED));
            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("이미 체결된 주문이면 ALREADY_FILLED 실패 응답만 보낸다")
        @Test
        void givenAlreadyFilled_whenCancelling_thenPublishesAlreadyFilled() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 0, 0L, null), OrderStatus.FILLED);

            sut.registerCancel(request());

            then(cancelResponseEventPublisher).should().publish(argThat(e ->
                    !e.success() && e.errorCode() == CancelErrorCode.ALREADY_FILLED));
            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("현물 매수 주문 취소: 잔량 금액 + 남은 수수료를 환불하고, 취소 이력·대기열·응답을 처리한다")
        @Test
        void givenOpenSpotBuy_whenCancelling_thenRefundsCashAndCompletes() {
            // 70,000 × 10 + 수수료 105
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN);
            given(accountApiClient.refundReservedCash(USERNAME, 700_105L)).willReturn(true);

            sut.registerCancel(request());

            ArgumentCaptor<CancelEntity> cancelCaptor = ArgumentCaptor.forClass(CancelEntity.class);
            then(cancelRepository).should().save(cancelCaptor.capture());
            assertThat(cancelCaptor.getValue().getOrderId()).isEqualTo(ORDER_ID);

            then(otocoOrderLifecycleListener).should().onOrderCanceled(ORDER_ID);
            then(orderQueueRegistry).should().orderCancel(ORDER_ID, STOCK_CODE);
            then(queuePositionBroadcastService).should().broadcast(STOCK_CODE, OrderType.BUY);
            then(stockServerOrderResponseRepository).should().delete(USERNAME, ORDER_ID);
            then(cancelResponseEventPublisher).should().publish(argThat(e ->
                    e.success() && e.errorCode() == null && e.orderId().equals(ORDER_ID)));
        }

        @DisplayName("부분 체결된 매수 주문은 남은 잔량 금액 + 남은 수수료만 환불한다")
        @Test
        void givenPartialSpotBuy_whenCancelling_thenRefundsRemainingOnly() {
            // 70,000 × 4 + 남은 수수료 42
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 4, 42L, null), OrderStatus.PARTIAL);
            given(accountApiClient.refundReservedCash(USERNAME, 280_042L)).willReturn(true);

            sut.registerCancel(request());

            then(accountApiClient).should().refundReservedCash(USERNAME, 280_042L);
        }

        @DisplayName("레버리지 매수 주문은 저장된 남은 증거금 + 남은 수수료를 환불한다")
        @Test
        void givenLeverageBuyWithReservedMargin_whenCancelling_thenRefundsReservedMargin() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.X2, 4, 42L, 140_000L), OrderStatus.PARTIAL);
            given(accountApiClient.refundReservedCash(USERNAME, 140_042L)).willReturn(true);

            sut.registerCancel(request());

            then(accountApiClient).should().refundReservedCash(USERNAME, 140_042L);
        }

        @DisplayName("저장된 증거금이 없는 레버리지 매수 주문(이전 데이터)은 잔량 금액으로 증거금을 계산해 환불한다")
        @Test
        void givenLeverageBuyWithoutReservedMargin_whenCancelling_thenCalculatesMargin() {
            // 280,000 × 0.5 = 140,000, 수수료 null → 0
            givenUserCancel(order(OrderType.BUY, LeverageRatio.X2, 4, null, null), OrderStatus.PARTIAL);
            given(accountApiClient.refundReservedCash(USERNAME, 140_000L)).willReturn(true);

            sut.registerCancel(request());

            then(accountApiClient).should().refundReservedCash(USERNAME, 140_000L);
        }

        @DisplayName("현물 매도 주문은 남은 수량만큼 보유 주식을 환불한다")
        @Test
        void givenSpotSell_whenCancelling_thenRefundsStock() {
            givenUserCancel(order(OrderType.SELL, LeverageRatio.SPOT, 7, null, null), OrderStatus.PARTIAL);
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 7)).willReturn(true);

            sut.registerCancel(request());

            then(accountApiClient).should(never()).refundReservedCash(anyString(), anyLong());
            then(queuePositionBroadcastService).should().broadcast(STOCK_CODE, OrderType.SELL);
        }

        @DisplayName("레버리지 매도 주문은 남은 수량만큼 레버리지 포지션을 환불한다")
        @Test
        void givenLeverageSell_whenCancelling_thenRefundsLeverageStock() {
            givenUserCancel(order(OrderType.SELL, LeverageRatio.X2, 7, null, null), OrderStatus.OPEN);
            given(accountApiClient.refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 7)).willReturn(true);

            sut.registerCancel(request());

            then(accountApiClient).should(never()).refundReservedStock(anyString(), anyString(), anyInt());
        }

        @DisplayName("DB 작업을 모두 끝낸 뒤 환불하고, 환불 성공 후에 대기열에서 제거한다")
        @Test
        void givenOpenOrder_whenCancelling_thenRefundsAfterDbWorkAndBeforeQueueRemoval() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN);
            given(accountApiClient.refundReservedCash(USERNAME, 700_105L)).willReturn(true);

            sut.registerCancel(request());

            InOrder inOrder = inOrder(cancelRepository, otocoOrderLifecycleListener, accountApiClient, orderQueueRegistry);
            inOrder.verify(cancelRepository).save(any(CancelEntity.class));
            inOrder.verify(otocoOrderLifecycleListener).onOrderCanceled(ORDER_ID);
            inOrder.verify(accountApiClient).refundReservedCash(USERNAME, 700_105L);
            inOrder.verify(orderQueueRegistry).orderCancel(ORDER_ID, STOCK_CODE);
        }

        @DisplayName("환불에 실패하면 내부 에러 응답 후 예외를 던지고, 대기열에서 제거하지 않는다 (롤백 대상)")
        @Test
        void givenRefundFails_whenCancelling_thenPublishesErrorAndRethrows() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN);
            given(accountApiClient.refundReservedCash(USERNAME, 700_105L)).willReturn(false);

            assertThatThrownBy(() -> sut.registerCancel(request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cash refund failed");

            then(orderQueueRegistry).should(never()).orderCancel(anyLong(), anyString());
            then(queuePositionBroadcastService).shouldHaveNoInteractions();
            then(stockServerOrderResponseRepository).shouldHaveNoInteractions();
            then(cancelResponseEventPublisher).should().publish(argThat(e ->
                    !e.success() && e.errorCode() == CancelErrorCode.INTERNAL_ERROR));
        }

        @DisplayName("주식 환불에 실패해도 예외를 던지고 대기열에서 제거하지 않는다")
        @Test
        void givenStockRefundFails_whenCancelling_thenRethrows() {
            givenUserCancel(order(OrderType.SELL, LeverageRatio.SPOT, 7, null, null), OrderStatus.OPEN);
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 7)).willReturn(false);

            assertThatThrownBy(() -> sut.registerCancel(request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Stock refund failed");

            then(orderQueueRegistry).should(never()).orderCancel(anyLong(), anyString());
        }

        @DisplayName("취소 이력 저장이 실패하면 환불하지 않는다")
        @Test
        void givenCancelSaveFails_whenCancelling_thenDoesNotRefund() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN);
            given(cancelRepository.save(any(CancelEntity.class))).willThrow(new IllegalStateException("db error"));

            assertThatThrownBy(() -> sut.registerCancel(request()))
                    .isInstanceOf(IllegalStateException.class);

            then(accountApiClient).shouldHaveNoInteractions();
            then(orderQueueRegistry).should(never()).orderCancel(anyLong(), anyString());
        }

        @DisplayName("OTOCO 연동이 실패하면 환불하지 않는다")
        @Test
        void givenOtocoListenerFails_whenCancelling_thenDoesNotRefund() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN);
            willThrow(new IllegalStateException("otoco error"))
                    .given(otocoOrderLifecycleListener).onOrderCanceled(ORDER_ID);

            assertThatThrownBy(() -> sut.registerCancel(request()))
                    .isInstanceOf(IllegalStateException.class);

            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("환불 이후 대기열 순번 알림이 실패해도 예외 없이 성공 응답을 보낸다 (중복 환불 방지)")
        @Test
        void givenBroadcastFailsAfterRefund_whenCancelling_thenStillSucceeds() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN);
            given(accountApiClient.refundReservedCash(USERNAME, 700_105L)).willReturn(true);
            willThrow(new IllegalStateException("broadcast error"))
                    .given(queuePositionBroadcastService).broadcast(STOCK_CODE, OrderType.BUY);

            assertThatCode(() -> sut.registerCancel(request())).doesNotThrowAnyException();

            then(cancelResponseEventPublisher).should().publish(argThat(CancelResponseEvent::success));
        }

        @DisplayName("환불 이후 Redis 응답 삭제가 실패해도 예외 없이 성공 응답을 보낸다 (중복 환불 방지)")
        @Test
        void givenResponseDeleteFailsAfterRefund_whenCancelling_thenStillSucceeds() {
            givenUserCancel(order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN);
            given(accountApiClient.refundReservedCash(USERNAME, 700_105L)).willReturn(true);
            willThrow(new IllegalStateException("redis error"))
                    .given(stockServerOrderResponseRepository).delete(USERNAME, ORDER_ID);

            assertThatCode(() -> sut.registerCancel(request())).doesNotThrowAnyException();

            then(cancelResponseEventPublisher).should().publish(argThat(CancelResponseEvent::success));
        }
    }

    @Nested
    @DisplayName("시스템 강제 취소 (forceCancel)")
    class ForceCancel {

        @DisplayName("이전 상태가 열린 상태가 아니면 아무 처리도 하지 않는다")
        @Test
        void givenNotOpen_whenForceCancelling_thenDoesNothing() {
            given(orderService.updateOrderStatusByCancelEvent(ORDER_ID))
                    .willReturn(UpdateOrderStatusResult.of(
                            order(OrderType.BUY, LeverageRatio.SPOT, 0, 0L, null), OrderStatus.FILLED));

            sut.forceCancel(ORDER_ID);

            then(accountApiClient).shouldHaveNoInteractions();
            then(cancelRepository).shouldHaveNoInteractions();
            then(cancelResponseEventPublisher).shouldHaveNoInteractions();
        }

        @DisplayName("열린 주문은 소유자 검증 없이 환불·취소 처리 후 성공 응답을 보낸다")
        @Test
        void givenOpen_whenForceCancelling_thenCancelsAndPublishesSuccess() {
            given(orderService.updateOrderStatusByCancelEvent(ORDER_ID))
                    .willReturn(UpdateOrderStatusResult.of(
                            order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN));
            given(accountApiClient.refundReservedCash(USERNAME, 700_105L)).willReturn(true);

            sut.forceCancel(ORDER_ID);

            then(orderQueueRegistry).should().orderCancel(ORDER_ID, STOCK_CODE);
            then(cancelResponseEventPublisher).should().publish(argThat(CancelResponseEvent::success));
        }

        @DisplayName("[현재 동작] 환불에 실패해도 예외를 밖으로 던지지 않는다 (트랜잭션 처리는 후속 작업에서 재검토)")
        @Test
        void givenRefundFails_whenForceCancelling_thenSwallowsException() {
            given(orderService.updateOrderStatusByCancelEvent(ORDER_ID))
                    .willReturn(UpdateOrderStatusResult.of(
                            order(OrderType.BUY, LeverageRatio.SPOT, 10, 105L, null), OrderStatus.OPEN));
            given(accountApiClient.refundReservedCash(USERNAME, 700_105L)).willReturn(false);

            assertThatCode(() -> sut.forceCancel(ORDER_ID)).doesNotThrowAnyException();

            then(orderQueueRegistry).should(never()).orderCancel(anyLong(), anyString());
            then(cancelResponseEventPublisher).shouldHaveNoInteractions();
        }
    }

    // ===== helpers =====

    private CancelRequestEvent request() {
        return CancelRequestEvent.of(ORDER_ID, STOCK_CODE, USERNAME);
    }

    private void givenUserCancel(OrderEntity entity, OrderStatus previousStatus) {
        given(orderService.updateOrderStatusByUserCancel(ORDER_ID, USERNAME, STOCK_CODE))
                .willReturn(Optional.of(UpdateOrderStatusResult.of(entity, previousStatus)));
    }

    /** markCanceled 이후 상태(CANCELED)의 주문 엔티티. remaining·fee·margin은 취소 시점 잔여값. */
    private OrderEntity order(OrderType type, LeverageRatio ratio, int remaining, Long fee, Long margin) {
        OrderEntity entity = OrderEntity.of(
                USERNAME, STOCK_CODE, type, ratio,
                70_000, 10, OrderStatus.CANCELED, remaining,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null, fee, margin);
        entity.setOrderId(ORDER_ID);
        return entity;
    }
}

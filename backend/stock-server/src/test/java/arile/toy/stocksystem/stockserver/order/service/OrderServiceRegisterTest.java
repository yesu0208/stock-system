package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.event.publisher.OrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 주문 등록 테스트")
@ExtendWith(MockitoExtension.class)
class OrderServiceRegisterTest {

    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";
    private static final int PRICE = 70_000;
    private static final int QUANTITY = 10;
    // 주문금액 700,000 → 수수료 105
    private static final long FEE = 105L;

    @InjectMocks private OrderService sut;

    @Mock private OrderRepository orderRepository;
    @Mock private OrderQueueRegistry orderQueueRegistry;
    @Mock private OrderResponseEventPublisher orderResponseEventPublisher;
    @Mock private StockServerOrderResponseRepository stockServerOrderResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Mock private QueuePositionBroadcastService queuePositionBroadcastService;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @DisplayName("현물 매수 주문이면 주문금액+수수료를 예약하고, 저장·대기열 등록·응답 발행을 한다")
    @Test
    void givenSpotBuy_whenRegistering_thenReservesCashAndSavesOrder() {
        // Given
        var request = request(OrderType.BUY, LeverageRatio.SPOT);
        given(accountApiClient.reserveCash(USERNAME, 700_000L + FEE)).willReturn(true);
        givenSaveAssignsId();

        // When
        OrderEntity result = sut.registerOrder(request, false);

        // Then
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.getRemainingQuantity()).isEqualTo(QUANTITY);
        assertThat(result.getRemainingReservedFee()).isEqualTo(FEE);
        assertThat(result.getRemainingReservedMargin()).isNull();

        ArgumentCaptor<OrderDto> dtoCaptor = ArgumentCaptor.forClass(OrderDto.class);
        then(orderQueueRegistry).should().orderEnqueue(dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().orderId()).isEqualTo(1L);

        then(queuePositionBroadcastService).should().broadcast(STOCK_CODE, OrderType.BUY);
        then(stockServerOrderResponseRepository).should().save(any(StockServerOrderResponseMessage.class));
        then(orderResponseEventPublisher).should().publish(any(StockServerOrderResponseMessage.class));
        then(orderResponseEventPublisher).should(never()).publishError(any(), any());
    }

    @DisplayName("레버리지 매수 주문이면 증거금+수수료만 예약하고, 증거금을 주문에 저장한다")
    @Test
    void givenLeverageBuy_whenRegistering_thenReservesMarginPlusFee() {
        // Given: X2 → 증거금 350,000
        var request = request(OrderType.BUY, LeverageRatio.X2);
        given(accountApiClient.reserveCash(USERNAME, 350_000L + FEE)).willReturn(true);
        givenSaveAssignsId();

        // When
        OrderEntity result = sut.registerOrder(request, false);

        // Then
        assertThat(result.getRemainingReservedFee()).isEqualTo(FEE);
        assertThat(result.getRemainingReservedMargin()).isEqualTo(350_000L);
    }

    @DisplayName("레버리지 비율이 없으면 현물로 처리한다")
    @Test
    void givenNullLeverage_whenRegistering_thenTreatsAsSpot() {
        // Given
        var request = request(OrderType.BUY, null);
        given(accountApiClient.reserveCash(USERNAME, 700_000L + FEE)).willReturn(true);
        givenSaveAssignsId();

        // When
        OrderEntity result = sut.registerOrder(request, false);

        // Then
        assertThat(result.getLeverageRatio()).isEqualTo(LeverageRatio.SPOT);
        assertThat(result.getRemainingReservedMargin()).isNull();
    }

    @DisplayName("매수 현금 예약에 실패하면 잔고 부족 에러를 발행하고 주문을 저장하지 않는다")
    @Test
    void givenReserveCashFails_whenRegistering_thenPublishesInsufficientBalance() {
        // Given
        var request = request(OrderType.BUY, LeverageRatio.SPOT);
        given(accountApiClient.reserveCash(USERNAME, 700_000L + FEE)).willReturn(false);

        // When
        OrderEntity result = sut.registerOrder(request, false);

        // Then
        assertThat(result).isNull();
        then(orderResponseEventPublisher).should().publishError(request, OrderErrorCode.INSUFFICIENT_BALANCE);
        then(orderRepository).shouldHaveNoInteractions();
        then(orderQueueRegistry).shouldHaveNoInteractions();
    }

    @DisplayName("현물 매도 주문이면 보유 주식을 예약하고, 수수료·증거금은 저장하지 않는다")
    @Test
    void givenSpotSell_whenRegistering_thenReservesStock() {
        // Given
        var request = request(OrderType.SELL, LeverageRatio.SPOT);
        given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, QUANTITY)).willReturn(true);
        givenSaveAssignsId();

        // When
        OrderEntity result = sut.registerOrder(request, false);

        // Then
        assertThat(result.getRemainingReservedFee()).isNull();
        assertThat(result.getRemainingReservedMargin()).isNull();
        then(accountApiClient).should(never()).reserveCash(any(), anyLong());
        then(queuePositionBroadcastService).should().broadcast(STOCK_CODE, OrderType.SELL);
    }

    @DisplayName("레버리지 매도 주문이면 레버리지 포지션 수량을 예약한다")
    @Test
    void givenLeverageSell_whenRegistering_thenReservesLeverageStock() {
        // Given
        var request = request(OrderType.SELL, LeverageRatio.X2);
        given(accountApiClient.reserveLeverageStock(USERNAME, STOCK_CODE, "X2", QUANTITY)).willReturn(true);
        givenSaveAssignsId();

        // When
        sut.registerOrder(request, false);

        // Then
        then(accountApiClient).should(never()).reserveStock(any(), any(), anyInt());
    }

    @DisplayName("매도 주식 예약에 실패하면 보유 주식 부족 에러를 발행하고 주문을 저장하지 않는다")
    @Test
    void givenReserveStockFails_whenRegistering_thenPublishesInsufficientStock() {
        // Given
        var request = request(OrderType.SELL, LeverageRatio.SPOT);
        given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, QUANTITY)).willReturn(false);

        // When
        OrderEntity result = sut.registerOrder(request, false);

        // Then
        assertThat(result).isNull();
        then(orderResponseEventPublisher).should().publishError(request, OrderErrorCode.INSUFFICIENT_STOCK);
        then(orderRepository).shouldHaveNoInteractions();
    }

    @DisplayName("자동주문에서 발생한 주문은 이미 예약이 끝났으므로 계좌 예약을 다시 하지 않는다")
    @Test
    void givenFromAutoOrder_whenRegistering_thenSkipsReservation() {
        // Given
        var request = request(OrderType.BUY, LeverageRatio.SPOT);
        givenSaveAssignsId();

        // When
        OrderEntity result = sut.registerOrder(request, true);

        // Then
        assertThat(result.getRemainingReservedFee()).isEqualTo(FEE);
        then(accountApiClient).shouldHaveNoInteractions();
        then(orderResponseEventPublisher).should().publish(any(StockServerOrderResponseMessage.class));
    }

    @DisplayName("대기열 등록에 실패하면 주문을 취소 상태로 저장하고, 현금을 환불하고, 내부 에러를 발행한 뒤 예외를 다시 던진다")
    @Test
    void givenBuyAndEnqueueFails_whenRegistering_thenCancelsOrderRefundsCashAndRethrows() {
        // Given
        var request = request(OrderType.BUY, LeverageRatio.SPOT);
        given(accountApiClient.reserveCash(USERNAME, 700_000L + FEE)).willReturn(true);
        givenSaveAssignsId();
        willThrow(new IllegalStateException("queue error")).given(orderQueueRegistry).orderEnqueue(any());

        // When & Then
        assertThatThrownBy(() -> sut.registerOrder(request, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("queue error");

        ArgumentCaptor<OrderEntity> entityCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        then(orderRepository).should(times(2)).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getOrderStatus()).isEqualTo(OrderStatus.CANCELED);

        then(orderQueueRegistry).should().orderCancel(1L, STOCK_CODE);
        then(accountApiClient).should().refundReservedCash(USERNAME, 700_000L + FEE);
        then(orderResponseEventPublisher).should().publishError(request, OrderErrorCode.INTERNAL_ERROR);
        then(orderResponseEventPublisher).should(never()).publish(any());
        then(stockServerOrderResponseRepository).shouldHaveNoInteractions();
    }

    @DisplayName("대기열 등록 후 브로드캐스트에 실패하면 대기열에서 주문을 제거하고 취소 상태로 저장한다")
    @Test
    void givenBroadcastFails_whenRegistering_thenRemovesFromQueueAndCancels() {
        // Given
        var request = request(OrderType.BUY, LeverageRatio.SPOT);
        given(accountApiClient.reserveCash(USERNAME, 700_000L + FEE)).willReturn(true);
        givenSaveAssignsId();
        willThrow(new IllegalStateException("broadcast error"))
                .given(queuePositionBroadcastService).broadcast(STOCK_CODE, OrderType.BUY);

        // When & Then
        assertThatThrownBy(() -> sut.registerOrder(request, false))
                .isInstanceOf(IllegalStateException.class);

        then(orderQueueRegistry).should().orderEnqueue(any());
        then(orderQueueRegistry).should().orderCancel(1L, STOCK_CODE);

        ArgumentCaptor<OrderEntity> entityCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        then(orderRepository).should(times(2)).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        then(accountApiClient).should().refundReservedCash(USERNAME, 700_000L + FEE);
    }

    @DisplayName("취소 상태 저장까지 실패해도 원래 예외에 첨부한 뒤, 환불과 에러 발행은 그대로 진행한다")
    @Test
    void givenCancelSaveFails_whenRegistering_thenAttachesSuppressedAndStillRefunds() {
        // Given
        var request = request(OrderType.BUY, LeverageRatio.SPOT);
        given(accountApiClient.reserveCash(USERNAME, 700_000L + FEE)).willReturn(true);
        given(orderRepository.save(any(OrderEntity.class)))
                .willAnswer(invocation -> {
                    OrderEntity entity = invocation.getArgument(0);
                    entity.setOrderId(1L);
                    return entity;
                })
                .willThrow(new IllegalStateException("cancel save error"));
        willThrow(new IllegalStateException("queue error")).given(orderQueueRegistry).orderEnqueue(any());

        // When & Then
        assertThatThrownBy(() -> sut.registerOrder(request, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("queue error")
                .satisfies(e -> assertThat(e.getSuppressed())
                        .singleElement()
                        .extracting(Throwable::getMessage)
                        .isEqualTo("cancel save error"));

        then(accountApiClient).should().refundReservedCash(USERNAME, 700_000L + FEE);
        then(orderResponseEventPublisher).should().publishError(request, OrderErrorCode.INTERNAL_ERROR);
    }

    @DisplayName("현물 매도 주문 저장 자체가 실패하면 취소 처리 없이 예약한 주식만 환불한다")
    @Test
    void givenSpotSellAndSaveFails_whenRegistering_thenRefundsStockWithoutCancel() {
        // Given
        var request = request(OrderType.SELL, LeverageRatio.SPOT);
        given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, QUANTITY)).willReturn(true);
        given(orderRepository.save(any(OrderEntity.class))).willThrow(new IllegalStateException("db error"));

        // When & Then
        assertThatThrownBy(() -> sut.registerOrder(request, false))
                .isInstanceOf(IllegalStateException.class);

        then(orderRepository).should(times(1)).save(any(OrderEntity.class));
        then(orderQueueRegistry).shouldHaveNoInteractions();
        then(accountApiClient).should().refundReservedStock(USERNAME, STOCK_CODE, QUANTITY);
        then(orderResponseEventPublisher).should().publishError(request, OrderErrorCode.INTERNAL_ERROR);
    }

    @DisplayName("레버리지 매도 주문 저장 중 예외가 나면 예약한 레버리지 포지션을 환불한다")
    @Test
    void givenLeverageSellAndSaveFails_whenRegistering_thenRefundsLeverageStock() {
        // Given
        var request = request(OrderType.SELL, LeverageRatio.X2);
        given(accountApiClient.reserveLeverageStock(USERNAME, STOCK_CODE, "X2", QUANTITY)).willReturn(true);
        given(orderRepository.save(any(OrderEntity.class))).willThrow(new IllegalStateException("db error"));

        // When & Then
        assertThatThrownBy(() -> sut.registerOrder(request, false))
                .isInstanceOf(IllegalStateException.class);

        then(accountApiClient).should().refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", QUANTITY);
        then(accountApiClient).should(never()).refundReservedStock(any(), any(), anyInt());
    }

    @DisplayName("자동주문에서 발생한 주문은 예외가 나도 환불하지 않고 내부 에러만 발행한다")
    @Test
    void givenFromAutoOrderAndSaveFails_whenRegistering_thenDoesNotRefund() {
        // Given
        var request = request(OrderType.BUY, LeverageRatio.SPOT);
        given(orderRepository.save(any(OrderEntity.class))).willThrow(new IllegalStateException("db error"));

        // When & Then
        assertThatThrownBy(() -> sut.registerOrder(request, true))
                .isInstanceOf(IllegalStateException.class);

        then(accountApiClient).shouldHaveNoInteractions();
        then(orderResponseEventPublisher).should().publishError(request, OrderErrorCode.INTERNAL_ERROR);
    }

    // ===== helpers =====

    private StockServerOrderRequestEvent request(OrderType orderType, LeverageRatio leverageRatio) {
        return new StockServerOrderRequestEvent(
                USERNAME, STOCK_CODE, orderType, PRICE, QUANTITY, leverageRatio,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);
    }

    private void givenSaveAssignsId() {
        given(orderRepository.save(any(OrderEntity.class))).willAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            entity.setOrderId(1L);
            return entity;
        });
    }
}

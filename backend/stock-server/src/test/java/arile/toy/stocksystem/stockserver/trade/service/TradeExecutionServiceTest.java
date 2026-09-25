package arile.toy.stocksystem.stockserver.trade.service;

import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoOrderLifecycleListener;
import arile.toy.stocksystem.stockserver.trade.dto.TradeResult;
import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import arile.toy.stocksystem.stockserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.stockserver.trade.outbox.service.TradeOutboxRecorder;
import arile.toy.stocksystem.stockserver.trade.repository.TradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 체결 실행(DB 반영) 테스트")
@ExtendWith(MockitoExtension.class)
class TradeExecutionServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long TRADE_ID = 100L;
    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";

    @InjectMocks private TradeExecutionService sut;

    @Mock private TradeRepository tradeRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private TradeOutboxRecorder tradeOutboxRecorder;
    @Mock private OtocoOrderLifecycleListener otocoOrderLifecycleListener;

    @DisplayName("주문이 없으면 예외를 던진다")
    @Test
    void givenOrderNotFound_whenExecuting_thenThrows() {
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.executeBuyTrade(dto(OrderType.BUY, LeverageRatio.SPOT, 10), 70_000, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order not found.");

        then(tradeRepository).shouldHaveNoInteractions();
    }

    @DisplayName("열린 상태(OPEN·PARTIAL)가 아닌 주문은 체결하지 않고 null을 반환한다")
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OrderStatus.class, names = {"CANCELED", "FILLED"})
    void givenClosedOrder_whenExecuting_thenReturnsNull(OrderStatus status) {
        OrderEntity entity = entity(OrderType.BUY, LeverageRatio.SPOT, status, 10, 105L, null);
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(entity));

        TradeResult result = sut.executeBuyTrade(dto(OrderType.BUY, LeverageRatio.SPOT, 10), 70_000, 4);

        assertThat(result).isNull();
        assertThat(entity.getRemainingReservedFee()).isEqualTo(105L);
        then(tradeRepository).shouldHaveNoInteractions();
        then(tradeOutboxRecorder).shouldHaveNoInteractions();
        then(otocoOrderLifecycleListener).shouldHaveNoInteractions();
    }

    @DisplayName("현물 매수 부분체결: 수수료를 비율대로 소진하고 PARTIAL로 바꾼 뒤 OTOCO·outbox에 알린다")
    @Test
    void givenSpotBuyPartialFill_whenExecuting_thenConsumesFeeAndMarksPartial() {
        // Given: 잔량 10주, 예약 수수료 105 → 4주 체결 시 round(105 × 4 / 10) = 42
        OrderEntity entity = entity(OrderType.BUY, LeverageRatio.SPOT, OrderStatus.OPEN, 10, 105L, null);
        givenOrder(entity);
        givenTradeSaved();

        // When
        TradeResult result = sut.executeBuyTrade(dto(OrderType.BUY, LeverageRatio.SPOT, 10), 70_000, 4);

        // Then
        assertThat(result.tradeEntity().getTradeId()).isEqualTo(TRADE_ID);

        ArgumentCaptor<TradeEntity> tradeCaptor = ArgumentCaptor.forClass(TradeEntity.class);
        then(tradeRepository).should().save(tradeCaptor.capture());
        TradeEntity trade = tradeCaptor.getValue();
        assertThat(trade.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(trade.getTradeType()).isEqualTo(TradeType.BUY);
        assertThat(trade.getTradePrice()).isEqualTo(70_000);
        assertThat(trade.getTradeQuantity()).isEqualTo(4);

        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.PARTIAL);
        assertThat(entity.getRemainingQuantity()).isEqualTo(6);
        assertThat(entity.getRemainingReservedFee()).isEqualTo(63L);
        then(orderRepository).should().save(entity);

        then(otocoOrderLifecycleListener).should().onOrderPartiallyFilled(ORDER_ID, 6);
        then(otocoOrderLifecycleListener).should(never()).onOrderFilled(anyLong());

        TradeExecutedEvent event = capturedOutboxEvent();
        assertThat(event.tradeId()).isEqualTo(TRADE_ID);
        assertThat(event.tradeType()).isEqualTo(TradeType.BUY);
        assertThat(event.orderPrice()).isEqualTo(70_000);
        assertThat(event.tradePrice()).isEqualTo(70_000);
        assertThat(event.tradeQuantity()).isEqualTo(4);
        assertThat(event.reservedFeeConsumed()).isEqualTo(42L);
        assertThat(event.reservedMarginConsumed()).isNull();
    }

    @DisplayName("현물 매수 전량체결: 남은 수수료를 모두 소진하고 FILLED로 바꾼 뒤 OTOCO에 체결 완료를 알린다")
    @Test
    void givenSpotBuyFullFill_whenExecuting_thenConsumesAllFeeAndMarksFilled() {
        OrderEntity entity = entity(OrderType.BUY, LeverageRatio.SPOT, OrderStatus.PARTIAL, 6, 63L, null);
        givenOrder(entity);
        givenTradeSaved();

        sut.executeBuyTrade(dto(OrderType.BUY, LeverageRatio.SPOT, 6), 70_000, 6);

        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(entity.getRemainingQuantity()).isZero();
        assertThat(entity.getRemainingReservedFee()).isZero();
        then(otocoOrderLifecycleListener).should().onOrderFilled(ORDER_ID);
        then(otocoOrderLifecycleListener).should(never()).onOrderPartiallyFilled(anyLong(), anyInt());
        assertThat(capturedOutboxEvent().reservedFeeConsumed()).isEqualTo(63L);
    }

    @DisplayName("레버리지 매수: 수수료와 함께 개시증거금도 비율대로 소진한다")
    @Test
    void givenLeverageBuy_whenExecuting_thenConsumesMarginToo() {
        // 증거금 350,000 → 4/10 체결 시 140,000
        OrderEntity entity = entity(OrderType.BUY, LeverageRatio.X2, OrderStatus.OPEN, 10, 105L, 350_000L);
        givenOrder(entity);
        givenTradeSaved();

        sut.executeBuyTrade(dto(OrderType.BUY, LeverageRatio.X2, 10), 70_000, 4);

        assertThat(entity.getRemainingReservedMargin()).isEqualTo(210_000L);
        TradeExecutedEvent event = capturedOutboxEvent();
        assertThat(event.reservedFeeConsumed()).isEqualTo(42L);
        assertThat(event.reservedMarginConsumed()).isEqualTo(140_000L);
        assertThat(event.leverageRatio()).isEqualTo(LeverageRatio.X2);
    }

    @DisplayName("매도: 수수료·증거금을 소진하지 않고(null) 체결만 반영한다")
    @Test
    void givenSell_whenExecuting_thenDoesNotConsumeReservations() {
        OrderEntity entity = entity(OrderType.SELL, LeverageRatio.X2, OrderStatus.OPEN, 10, null, null);
        givenOrder(entity);
        givenTradeSaved();

        sut.executeSellTrade(dto(OrderType.SELL, LeverageRatio.X2, 10), 70_000, 10);

        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.FILLED);
        TradeExecutedEvent event = capturedOutboxEvent();
        assertThat(event.tradeType()).isEqualTo(TradeType.SELL);
        assertThat(event.reservedFeeConsumed()).isNull();
        assertThat(event.reservedMarginConsumed()).isNull();
    }

    @DisplayName("outbox 기록이 실패하면 예외를 던져 체결 트랜잭션 전체가 롤백되게 한다")
    @Test
    void givenOutboxFails_whenExecuting_thenThrows() {
        OrderEntity entity = entity(OrderType.BUY, LeverageRatio.SPOT, OrderStatus.OPEN, 10, 105L, null);
        givenOrder(entity);
        givenTradeSaved();
        willThrow(new IllegalStateException("Outbox 기록 실패"))
                .given(tradeOutboxRecorder).record(any(TradeExecutedEvent.class));

        assertThatThrownBy(() -> sut.executeBuyTrade(dto(OrderType.BUY, LeverageRatio.SPOT, 10), 70_000, 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Outbox 기록 실패");
    }

    // ===== helpers =====

    private void givenOrder(OrderEntity entity) {
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(entity));
    }

    private void givenTradeSaved() {
        given(tradeRepository.save(any(TradeEntity.class))).willAnswer(invocation -> {
            TradeEntity trade = invocation.getArgument(0);
            trade.setTradeId(TRADE_ID);
            return trade;
        });
    }

    private TradeExecutedEvent capturedOutboxEvent() {
        ArgumentCaptor<TradeExecutedEvent> captor = ArgumentCaptor.forClass(TradeExecutedEvent.class);
        then(tradeOutboxRecorder).should().record(captor.capture());
        return captor.getValue();
    }

    private OrderEntity entity(OrderType type, LeverageRatio ratio, OrderStatus status,
                               int remaining, Long fee, Long margin) {
        OrderEntity entity = OrderEntity.of(
                USERNAME, STOCK_CODE, type, ratio,
                70_000, 10, status, remaining,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null, fee, margin);
        entity.setOrderId(ORDER_ID);
        return entity;
    }

    private OrderDto dto(OrderType type, LeverageRatio ratio, int remaining) {
        return new OrderDto(ORDER_ID, USERNAME, STOCK_CODE, type, ratio,
                70_000, 10, remaining, OrderStatus.OPEN, Instant.parse("2026-09-25T00:00:00Z"),
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);
    }
}

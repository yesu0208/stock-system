package arile.toy.stocksystem.stockserver.trade.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.StockLockRegistry;
import arile.toy.stocksystem.stockserver.order.dto.*;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.order.service.QueuePositionBroadcastService;
import arile.toy.stocksystem.stockserver.trade.dto.TradeResult;
import arile.toy.stocksystem.stockserver.trade.dto.TradeType;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;
import arile.toy.stocksystem.stockserver.trade.event.publisher.TradeResponseEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 체결 매칭 테스트")
@ExtendWith(MockitoExtension.class)
class TradeMatchingServiceTest {

    private static final String STOCK_CODE = "005930";
    private static final Instant T0 = Instant.parse("2026-09-25T00:00:00Z");

    @InjectMocks private TradeMatchingService sut;

    @Spy private StockLockRegistry stockLockRegistry = new StockLockRegistry();
    @Spy private OrderQueueRegistry orderQueueRegistry = new OrderQueueRegistry();
    @Mock private TradeExecutionService tradeExecutionService;
    @Mock private StockServerOrderResponseRepository stockServerOrderResponseRepository;
    @Mock private TradeResponseEventPublisher tradeResponseEventPublisher;
    @Mock private QueuePositionBroadcastService queuePositionBroadcastService;

    @Nested
    @DisplayName("매수 체결 틱(1): 대기 중인 매도 주문 체결")
    class SellSide {

        @DisplayName("체결가 이하 매도 주문을 가격 우선으로 체결하고, 틱 수량을 넘는 잔량은 부분체결로 대기열에 남긴다")
        @Test
        void givenSellOrders_whenTick_thenFillsByPricePriorityAndKeepsPartial() {
            // Given: 69,000원 5주, 70,000원 5주 / 틱 70,000원 8주
            enqueue(order(1L, OrderType.SELL, 69_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            givenSellExecutes(1L, 70_000, 5);
            givenSellExecutes(2L, 70_000, 3);

            // When
            sut.getExternalTickMessageAndTrade(tick(70_000, 8, "1"));

            // Then
            then(tradeResponseEventPublisher).should(times(2)).publish(any(TradeResponseEvent.class));
            then(stockServerOrderResponseRepository).should().delete("user", 1L);
            then(stockServerOrderResponseRepository).should()
                    .update(eq("user"), eq(2L), argThat(m -> m.remainingQuantity() == 2));

            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL))
                    .extracting(OrderDto::orderId, OrderDto::remainingQuantity, OrderDto::orderStatus)
                    .containsExactly(tuple(2L, 2, OrderStatus.PARTIAL));
        }

        @DisplayName("같은 가격이면 먼저 들어온 주문부터 체결한다")
        @Test
        void givenSamePrice_whenTick_thenFillsEarlierFirst() {
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0.plusSeconds(1)));
            enqueue(order(1L, OrderType.SELL, 70_000, 5, T0));
            givenSellExecutes(1L, 70_000, 5);

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "1"));

            then(tradeExecutionService).should().executeSellTrade(argThat(o -> o.orderId() == 1L), eq(70_000), eq(5));
            then(tradeExecutionService).should(never()).executeSellTrade(argThat(o -> o.orderId() == 2L), anyInt(), anyInt());
        }

        @DisplayName("가장 싼 매도 주문도 체결가보다 비싸면 체결하지 않고 대기열에 그대로 둔다")
        @Test
        void givenSellAboveTradePrice_whenTick_thenKeepsInQueue() {
            enqueue(order(1L, OrderType.SELL, 71_000, 5, T0));

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "1"));

            then(tradeExecutionService).shouldHaveNoInteractions();
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL)).hasSize(1);
        }

        @DisplayName("이미 취소된 주문(null)은 건너뛰고 틱 수량을 소모하지 않은 채 다음 주문을 체결한다")
        @Test
        void givenCanceledOrder_whenTick_thenSkipsAndContinues() {
            enqueue(order(1L, OrderType.SELL, 69_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            given(tradeExecutionService.executeSellTrade(argThat(o -> o != null && o.orderId() == 1L), eq(70_000), eq(5)))
                    .willReturn(null);
            givenSellExecutes(2L, 70_000, 5);

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "1"));

            then(tradeResponseEventPublisher).should(times(1)).publish(any(TradeResponseEvent.class));
            then(stockServerOrderResponseRepository).should().delete("user", 2L);
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL)).isEmpty();
        }

        @DisplayName("체결 실행이 실패하면 꺼낸 주문을 대기열로 되돌리고 이번 틱 매칭을 멈춘다")
        @Test
        void givenExecutionFails_whenTick_thenRestoresOrderAndStops() {
            enqueue(order(1L, OrderType.SELL, 69_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            given(tradeExecutionService.executeSellTrade(argThat(o -> o != null && o.orderId() == 1L), eq(70_000), eq(5)))
                    .willThrow(new IllegalStateException("db error"));

            assertThatCode(() -> sut.getExternalTickMessageAndTrade(tick(70_000, 10, "1")))
                    .doesNotThrowAnyException();

            then(tradeExecutionService).should(times(1)).executeSellTrade(any(), anyInt(), anyInt());
            then(tradeResponseEventPublisher).shouldHaveNoInteractions();
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL))
                    .extracting(OrderDto::orderId, OrderDto::remainingQuantity)
                    .containsExactly(tuple(1L, 5), tuple(2L, 5));
        }

        @DisplayName("체결 후 Redis 응답 갱신·순번 알림이 실패해도 매칭을 계속한다")
        @Test
        void givenPostTradeSideEffectsFail_whenTick_thenContinuesMatching() {
            enqueue(order(1L, OrderType.SELL, 69_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            givenSellExecutes(1L, 70_000, 5);
            givenSellExecutes(2L, 70_000, 5);
            willThrow(new IllegalStateException("redis")).given(stockServerOrderResponseRepository)
                    .delete(anyString(), anyLong());
            willThrow(new IllegalStateException("broadcast")).given(queuePositionBroadcastService)
                    .broadcast(anyString(), any());

            assertThatCode(() -> sut.getExternalTickMessageAndTrade(tick(70_000, 10, "1")))
                    .doesNotThrowAnyException();

            then(tradeExecutionService).should(times(2)).executeSellTrade(any(), anyInt(), anyInt());
        }

        @DisplayName("대기 주문이 없으면 아무것도 하지 않는다")
        @Test
        void givenEmptyQueue_whenTick_thenDoesNothing() {
            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "1"));

            then(tradeExecutionService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("매도 체결 틱(5): 대기 중인 매수 주문 체결")
    class BuySide {

        @DisplayName("체결가 이상 매수 주문을 체결하고 전량 체결되면 Redis 응답을 삭제한다")
        @Test
        void givenBuyAtOrAboveTradePrice_whenTick_thenFills() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            givenBuyExecutes(1L, 70_000, 5);

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "5"));

            then(tradeResponseEventPublisher).should().publish(any(TradeResponseEvent.class));
            then(stockServerOrderResponseRepository).should().delete("user", 1L);
            then(queuePositionBroadcastService).should().broadcast(STOCK_CODE, OrderType.BUY);
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY)).isEmpty();
        }

        @DisplayName("가장 비싼 매수 주문도 체결가보다 싸면 체결하지 않고 대기열에 그대로 둔다")
        @Test
        void givenBuyBelowTradePrice_whenTick_thenKeepsInQueue() {
            enqueue(order(1L, OrderType.BUY, 69_000, 5, T0));

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "5"));

            then(tradeExecutionService).shouldHaveNoInteractions();
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY)).hasSize(1);
        }

        @DisplayName("체결 실행이 실패하면 꺼낸 매수 주문을 대기열로 되돌린다")
        @Test
        void givenExecutionFails_whenTick_thenRestoresOrder() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            given(tradeExecutionService.executeBuyTrade(any(), eq(70_000), eq(5)))
                    .willThrow(new IllegalStateException("db error"));

            assertThatCode(() -> sut.getExternalTickMessageAndTrade(tick(70_000, 5, "5")))
                    .doesNotThrowAnyException();

            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY))
                    .extracting(OrderDto::orderId)
                    .containsExactly(1L);
        }

        @DisplayName("대기 중인 매수 주문이 없으면 아무것도 체결하지 않는다")
        @Test
        void givenNoBuyOrders_whenTick_thenNothing() {
            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "5"));

            then(tradeExecutionService).shouldHaveNoInteractions();
            then(tradeResponseEventPublisher).shouldHaveNoInteractions();
        }

        @DisplayName("이미 취소된 매수 주문(null)은 건너뛰고 다음 매수 주문을 체결한다")
        @Test
        void givenCanceledBuy_whenTick_thenSkipsAndContinues() {
            enqueue(order(1L, OrderType.BUY, 71_000, 5, T0));
            enqueue(order(2L, OrderType.BUY, 70_000, 5, T0));
            given(tradeExecutionService.executeBuyTrade(argThat(o -> o != null && o.orderId() == 1L), eq(70_000), eq(5)))
                    .willReturn(null);
            givenBuyExecutes(2L, 70_000, 5);

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "5"));

            then(tradeResponseEventPublisher).should(times(1)).publish(any(TradeResponseEvent.class));
            then(stockServerOrderResponseRepository).should().delete("user", 2L);
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("동시호가 틱(3): 매수·매도 동시 체결")
    class CallAuction {

        @DisplayName("매수·매도가 모두 가격 조건을 만족하면 양쪽을 체결하고 각각 체결 알림을 발행한다")
        @Test
        void givenBothSidesMatch_whenTick_thenFillsBothAndPublishes() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            givenSellExecutes(2L, 70_000, 5);
            givenBuyExecutes(1L, 70_000, 5);

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3"));

            ArgumentCaptor<TradeResponseEvent> captor = ArgumentCaptor.forClass(TradeResponseEvent.class);
            then(tradeResponseEventPublisher).should(times(2)).publish(captor.capture());
            assertThat(captor.getAllValues()).extracting(TradeResponseEvent::orderId)
                    .containsExactlyInAnyOrder(1L, 2L);
        }

        @DisplayName("빈 문자열 체결 구분도 동시호가로 처리한다")
        @Test
        void givenEmptyTradingType_whenTick_thenTreatedAsCallAuction() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            givenSellExecutes(2L, 70_000, 5);
            givenBuyExecutes(1L, 70_000, 5);

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, ""));

            then(tradeExecutionService).should().executeSellTrade(any(), eq(70_000), eq(5));
            then(tradeExecutionService).should().executeBuyTrade(any(), eq(70_000), eq(5));
        }

        @DisplayName("한쪽 대기열만 있으면 체결하지 않고 주문을 되돌린다 (동시호가는 매수·매도가 모두 있어야 체결)")
        @Test
        void givenOnlyBuySide_whenTick_thenRestoresWithoutExecution() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3"));

            then(tradeExecutionService).shouldHaveNoInteractions();
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY)).hasSize(1);
        }

        @DisplayName("한쪽이라도 가격 조건이 맞지 않으면 양쪽 모두 되돌린다 (동시호가는 매수·매도가 모두 있어야 체결)")
        @Test
        void givenOneSidePriceMismatch_whenTick_thenRestoresBoth() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 71_000, 5, T0));

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3"));

            then(tradeExecutionService).shouldHaveNoInteractions();
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY)).hasSize(1);
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL)).hasSize(1);
        }

        @DisplayName("매도 체결이 실패하면 매수·매도 모두 대기열로 되돌린다")
        @Test
        void givenSellExecutionFails_whenTick_thenRestoresBoth() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            given(tradeExecutionService.executeSellTrade(any(), eq(70_000), eq(5)))
                    .willThrow(new IllegalStateException("db error"));

            assertThatCode(() -> sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3")))
                    .doesNotThrowAnyException();

            then(tradeExecutionService).should(never()).executeBuyTrade(any(), anyInt(), anyInt());
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY)).hasSize(1);
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL)).hasSize(1);
        }

        @DisplayName("매도 체결 후 매수 체결이 실패하면 매수 주문만 대기열로 되돌린다")
        @Test
        void givenBuyExecutionFailsAfterSell_whenTick_thenRestoresBuyOnly() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            givenSellExecutes(2L, 70_000, 5);
            given(tradeExecutionService.executeBuyTrade(any(), eq(70_000), eq(5)))
                    .willThrow(new IllegalStateException("db error"));

            assertThatCode(() -> sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3")))
                    .doesNotThrowAnyException();

            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY))
                    .extracting(OrderDto::orderId).containsExactly(1L);
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL)).isEmpty();
        }

        @DisplayName("매수나 매도 한쪽만 있으면 체결하지 않고 꺼낸 주문을 되돌린다")
        @Test
        void givenOnlyOneSide_whenTick_thenRestores() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3"));

            orderQueueRegistry.pollBuy(STOCK_CODE);
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3"));

            then(tradeExecutionService).shouldHaveNoInteractions();
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY)).isEmpty();
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL))
                    .extracting(OrderDto::orderId).containsExactly(2L);
        }

        @DisplayName("매수가가 체결가보다 낮거나 매도가가 체결가보다 높으면 체결하지 않고 양쪽을 되돌린다")
        @ParameterizedTest
        @CsvSource({"69000, 70000", "70000, 71000"})
        void givenPriceMismatch_whenTick_thenRestoresBoth(int buyPrice, int sellPrice) {
            enqueue(order(1L, OrderType.BUY, buyPrice, 5, T0));
            enqueue(order(2L, OrderType.SELL, sellPrice, 5, T0));

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3"));

            then(tradeExecutionService).shouldHaveNoInteractions();
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.BUY)).hasSize(1);
            assertThat(orderQueueRegistry.snapshotRanked(STOCK_CODE, OrderType.SELL)).hasSize(1);
        }

        @DisplayName("매도 주문이 이미 취소됐으면(null) 매도는 건너뛰고 매수만 체결한다")
        @Test
        void givenCanceledSell_whenTick_thenOnlyBuyFills() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            given(tradeExecutionService.executeSellTrade(any(), eq(70_000), eq(5))).willReturn(null);
            givenBuyExecutes(1L, 70_000, 5);

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3"));

            then(tradeResponseEventPublisher).should(times(1)).publish(any(TradeResponseEvent.class));
            then(stockServerOrderResponseRepository).should().delete("user", 1L);
            then(stockServerOrderResponseRepository).should(never()).delete("user", 2L);
        }

        @DisplayName("매수 주문이 이미 취소됐으면(null) 매수는 건너뛰고 매도만 체결한다")
        @Test
        void givenCanceledBuy_whenTick_thenOnlySellFills() {
            enqueue(order(1L, OrderType.BUY, 70_000, 5, T0));
            enqueue(order(2L, OrderType.SELL, 70_000, 5, T0));
            givenSellExecutes(2L, 70_000, 5);
            given(tradeExecutionService.executeBuyTrade(any(), eq(70_000), eq(5))).willReturn(null);

            sut.getExternalTickMessageAndTrade(tick(70_000, 5, "3"));

            then(tradeResponseEventPublisher).should(times(1)).publish(any(TradeResponseEvent.class));
            then(stockServerOrderResponseRepository).should().delete("user", 2L);
            then(stockServerOrderResponseRepository).should(never()).delete("user", 1L);
        }
    }

    @DisplayName("알 수 없는 체결 구분이면 아무것도 하지 않는다")
    @Test
    void givenUnknownTradingType_whenTick_thenDoesNothing() {
        enqueue(order(1L, OrderType.SELL, 69_000, 5, T0));

        sut.getExternalTickMessageAndTrade(tick(70_000, 5, "2"));

        then(tradeExecutionService).shouldHaveNoInteractions();
    }

    // ===== helpers =====

    private void enqueue(OrderDto order) {
        orderQueueRegistry.orderEnqueue(order);
    }

    private void givenSellExecutes(Long orderId, int price, int quantity) {
        given(tradeExecutionService.executeSellTrade(
                argThat(o -> o != null && o.orderId().equals(orderId)), eq(price), eq(quantity)))
                .willReturn(result(orderId, TradeType.SELL, price, quantity));
    }

    private void givenBuyExecutes(Long orderId, int price, int quantity) {
        given(tradeExecutionService.executeBuyTrade(
                argThat(o -> o != null && o.orderId().equals(orderId)), eq(price), eq(quantity)))
                .willReturn(result(orderId, TradeType.BUY, price, quantity));
    }

    private TradeResult result(Long orderId, TradeType type, int price, int quantity) {
        TradeEntity entity = TradeEntity.of(orderId, "user", STOCK_CODE, type, price, quantity,
                LeverageRatio.SPOT, OrderOrigin.MANUAL, null);
        return TradeResult.of(entity);
    }

    private OrderDto order(Long orderId, OrderType type, int price, int remaining, Instant time) {
        return new OrderDto(orderId, "user", STOCK_CODE, type, LeverageRatio.SPOT,
                price, remaining, remaining, OrderStatus.OPEN, time,
                OrderExecutionType.LIMIT, OrderOrigin.MANUAL, null);
    }

    private TradePriceTickMessage tick(int price, int volume, String tradingType) {
        return new TradePriceTickMessage(null, STOCK_CODE, "090000", price,
                0, 0, "0", 0, 0, 0, volume, 0, 0L, 0, 0, tradingType, 0);
    }
}

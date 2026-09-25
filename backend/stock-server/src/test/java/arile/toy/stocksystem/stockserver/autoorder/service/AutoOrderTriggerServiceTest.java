package arile.toy.stocksystem.stockserver.autoorder.service;

import arile.toy.stocksystem.stockserver.autoorder.dto.*;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.AutoStockLockRegistry;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.dto.OrderOrigin;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 자동주문 발동 테스트")
@ExtendWith(MockitoExtension.class)
class AutoOrderTriggerServiceTest {

    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";
    private static final Instant T0 = Instant.parse("2026-09-25T00:00:00Z");

    @InjectMocks private AutoOrderTriggerService sut;

    @Spy private AutoOrderQueueRegistry autoOrderQueueRegistry = new AutoOrderQueueRegistry();
    @Spy private AutoStockLockRegistry autoStockLockRegistry = new AutoStockLockRegistry();
    @Mock private AutoOrderService autoOrderService;
    @Mock private StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    @Mock private OrderService orderService;
    @Mock private AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;
    @Mock private AccountApiClient accountApiClient;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @DisplayName("매수: 현재가가 발동가 이상이면 발동해 자동주문 출처로 주문을 등록하고 응답 삭제·발동 알림을 보낸다")
    @Test
    void givenBuyReached_whenTick_thenRegistersOrder() {
        enqueue(dto(1L, AutoOrderType.BUY, 70_000));
        givenTriggerable(1L);

        sut.getExternalTickMessageAndTrigger(tick(70_000));

        ArgumentCaptor<StockServerOrderRequestEvent> captor = ArgumentCaptor.forClass(StockServerOrderRequestEvent.class);
        then(orderService).should().registerOrder(captor.capture(), eq(true));
        StockServerOrderRequestEvent event = captor.getValue();
        assertThat(event.orderType()).isEqualTo(OrderType.BUY);
        assertThat(event.orderPrice()).isEqualTo(69_500);
        assertThat(event.origin()).isEqualTo(OrderOrigin.AUTO_ORDER);
        assertThat(event.originId()).isEqualTo(1L);

        then(stockServerAutoOrderResponseRepository).should().delete(USERNAME, 1L);
        then(autoOrderResponseEventPublisher).should().publishTrigger(USERNAME);
        assertThat(autoOrderQueueRegistry.peekBuy(STOCK_CODE)).isEmpty();
    }

    @DisplayName("매수: 발동가 낮은 순으로 발동하고, 발동가에 못 미치는 첫 주문에서 멈춰 대기열에 되돌린다")
    @Test
    void givenBuyOrders_whenTick_thenTriggersUpToCurrentPrice() {
        enqueue(dto(1L, AutoOrderType.BUY, 69_000));
        enqueue(dto(2L, AutoOrderType.BUY, 70_000));
        enqueue(dto(3L, AutoOrderType.BUY, 71_000));
        givenTriggerable(1L);
        givenTriggerable(2L);

        sut.getExternalTickMessageAndTrigger(tick(70_000));

        then(orderService).should(times(2)).registerOrder(any(), eq(true));
        assertThat(autoOrderQueueRegistry.peekBuy(STOCK_CODE)).get()
                .extracting(AutoOrderDto::autoOrderId).isEqualTo(3L);
    }

    @DisplayName("매도: 현재가가 발동가 이하이면 발동하고, 발동가 높은 순으로 처리한다")
    @Test
    void givenSellOrders_whenTick_thenTriggersFromHighestTrigger() {
        enqueue(dto(1L, AutoOrderType.SELL, 71_000));
        enqueue(dto(2L, AutoOrderType.SELL, 70_000));
        enqueue(dto(3L, AutoOrderType.SELL, 69_000));
        givenTriggerable(1L);
        givenTriggerable(2L);

        sut.getExternalTickMessageAndTrigger(tick(70_000));

        then(orderService).should(times(2)).registerOrder(argThat(e -> e.orderType() == OrderType.SELL), eq(true));
        assertThat(autoOrderQueueRegistry.peekSell(STOCK_CODE)).get()
                .extracting(AutoOrderDto::autoOrderId).isEqualTo(3L);
    }

    @DisplayName("이미 취소·발동된 자동주문은 주문을 등록하지 않고 다음 자동주문을 계속 처리한다")
    @Test
    void givenNotActive_whenTick_thenSkipsAndContinues() {
        enqueue(dto(1L, AutoOrderType.BUY, 69_000));
        enqueue(dto(2L, AutoOrderType.BUY, 70_000));
        given(autoOrderService.updateAutoOrderStatusByTrigger(1L))
                .willReturn(UpdateAutoOrderStatusResult.of(new AutoOrderEntity(), AutoOrderStatus.CANCELED));
        givenTriggerable(2L);

        sut.getExternalTickMessageAndTrigger(tick(70_000));

        then(orderService).should(times(1)).registerOrder(argThat(e -> e.originId().equals(2L)), eq(true));
    }

    @DisplayName("발동 상태 변경이 실패하면 자동주문을 대기열로 되돌리고 이번 틱 처리를 멈춘다")
    @Test
    void givenStatusUpdateFails_whenTick_thenRestoresAndStops() {
        enqueue(dto(1L, AutoOrderType.BUY, 69_000));
        enqueue(dto(2L, AutoOrderType.BUY, 70_000));
        given(autoOrderService.updateAutoOrderStatusByTrigger(1L)).willThrow(new IllegalStateException("db error"));

        assertThatCode(() -> sut.getExternalTickMessageAndTrigger(tick(70_000))).doesNotThrowAnyException();

        then(autoOrderService).should(times(1)).updateAutoOrderStatusByTrigger(anyLong());
        then(orderService).shouldHaveNoInteractions();
        assertThat(autoOrderQueueRegistry.pollBuy(STOCK_CODE).autoOrderId()).isEqualTo(1L);
        assertThat(autoOrderQueueRegistry.pollBuy(STOCK_CODE).autoOrderId()).isEqualTo(2L);
    }

    @DisplayName("주문 등록이 실패하면 매수 예약금(현물: 주문금액+수수료)을 환불하고 발동 실패를 알린다")
    @Test
    void givenRegisterFails_whenTick_thenRefundsCashAndPublishesFailure() {
        AutoOrderDto dto = dto(1L, AutoOrderType.BUY, 70_000);
        enqueue(dto);
        givenTriggerable(1L);
        given(orderService.registerOrder(any(), eq(true))).willThrow(new IllegalStateException("fail"));
        // 주문가 69,500 × 10 = 695,000 → 수수료 104
        given(accountApiClient.refundReservedCash(USERNAME, 695_104L)).willReturn(true);

        sut.getExternalTickMessageAndTrigger(tick(70_000));

        then(accountApiClient).should().refundReservedCash(USERNAME, 695_104L);
        then(stockServerAutoOrderResponseRepository).should().delete(USERNAME, 1L);
        then(autoOrderResponseEventPublisher).should().publishTriggerFailure(dto, AutoOrderResultCode.TRIGGER_FAILED);
        then(autoOrderResponseEventPublisher).should(never()).publishTrigger(anyString());
    }

    @DisplayName("주문 등록이 실패하면 매도는 보유 주식(현물)·레버리지 포지션을 환불한다")
    @Test
    void givenSellRegisterFails_whenTick_thenRefundsStock() {
        enqueue(dto(1L, AutoOrderType.SELL, 70_000));
        enqueue(new AutoOrderDto(2L, USERNAME, STOCK_CODE, AutoOrderType.SELL, LeverageRatio.X2,
                70_000, 69_500, 10, T0.plusSeconds(1)));
        givenTriggerable(1L);
        givenTriggerable(2L);
        given(orderService.registerOrder(any(), eq(true))).willThrow(new IllegalStateException("fail"));
        given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
        given(accountApiClient.refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10)).willReturn(true);

        sut.getExternalTickMessageAndTrigger(tick(70_000));

        then(accountApiClient).should().refundReservedStock(USERNAME, STOCK_CODE, 10);
        then(accountApiClient).should().refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10);
    }

    @DisplayName("보상 환불이 실패해도 발동 실패 알림은 보낸다")
    @Test
    void givenRefundFails_whenCompensating_thenStillPublishesFailure() {
        AutoOrderDto dto = dto(1L, AutoOrderType.BUY, 70_000);
        enqueue(dto);
        givenTriggerable(1L);
        given(orderService.registerOrder(any(), eq(true))).willThrow(new IllegalStateException("fail"));
        given(accountApiClient.refundReservedCash(anyString(), anyLong())).willReturn(false);

        sut.getExternalTickMessageAndTrigger(tick(70_000));

        then(autoOrderResponseEventPublisher).should().publishTriggerFailure(dto, AutoOrderResultCode.TRIGGER_FAILED);
    }

    @DisplayName("주문 등록 성공 후 Redis 응답 삭제가 실패해도 환불하지 않고 발동 알림을 보낸다 (이중 환불 방지)")
    @Test
    void givenDeleteFailsAfterRegister_whenTick_thenDoesNotRefund() {
        enqueue(dto(1L, AutoOrderType.BUY, 70_000));
        givenTriggerable(1L);
        willThrow(new IllegalStateException("redis down"))
                .given(stockServerAutoOrderResponseRepository).delete(USERNAME, 1L);

        assertThatCode(() -> sut.getExternalTickMessageAndTrigger(tick(70_000))).doesNotThrowAnyException();

        then(accountApiClient).shouldHaveNoInteractions();
        then(autoOrderResponseEventPublisher).should().publishTrigger(USERNAME);
        then(autoOrderResponseEventPublisher).should(never()).publishTriggerFailure(any(), any());
    }

    @DisplayName("발동 조건을 만족하는 자동주문이 없으면 아무것도 하지 않는다")
    @Test
    void givenNothingReached_whenTick_thenDoesNothing() {
        enqueue(dto(1L, AutoOrderType.BUY, 71_000));
        enqueue(dto(2L, AutoOrderType.SELL, 69_000));

        sut.getExternalTickMessageAndTrigger(tick(70_000));

        then(autoOrderService).shouldHaveNoInteractions();
        assertThat(autoOrderQueueRegistry.peekBuy(STOCK_CODE)).isPresent();
        assertThat(autoOrderQueueRegistry.peekSell(STOCK_CODE)).isPresent();
    }

    // ===== helpers =====

    private void enqueue(AutoOrderDto dto) {
        autoOrderQueueRegistry.autoOrderEnqueue(dto);
    }

    private void givenTriggerable(Long autoOrderId) {
        given(autoOrderService.updateAutoOrderStatusByTrigger(autoOrderId))
                .willReturn(UpdateAutoOrderStatusResult.of(new AutoOrderEntity(), AutoOrderStatus.ACTIVE));
    }

    private AutoOrderDto dto(Long id, AutoOrderType type, int triggerPrice) {
        return new AutoOrderDto(id, USERNAME, STOCK_CODE, type, LeverageRatio.SPOT,
                triggerPrice, 69_500, 10, T0.plusSeconds(id));
    }

    private TradePriceTickMessage tick(int price) {
        return new TradePriceTickMessage(null, STOCK_CODE, "100000", price,
                0, 0, "0", 0, 0, 0, 1, 0, 0L, 0, 0, "1", 0);
    }
}

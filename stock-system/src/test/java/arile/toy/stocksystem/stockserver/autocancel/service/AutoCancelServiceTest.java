package arile.toy.stocksystem.stockserver.autocancel.service;

import arile.toy.stocksystem.stockserver.autocancel.dto.AutoCancelErrorCode;
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
import arile.toy.stocksystem.stockserver.autoorder.sevice.AutoOrderService;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoCancelServiceTest {

    @Mock private AutoOrderService autoOrderService;
    @Mock
    private AutoCancelRepository autoCancelRepository;
    @Mock private AutoOrderQueueRegistry autoOrderQueueRegistry;
    @Mock private AutoCancelResponseEventPublisher autoCancelResponseEventPublisher;
    @Mock private StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    @Mock private AccountBalanceCommand accountBalanceCommand;
    @Mock private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private AutoCancelService autoCancelService;

    @Test
    @DisplayName("이미 취소된 AutoOrder에 대해 AutoCancel 요청 시 ALREADY_CANCELLED 이벤트 발행")
    void givenAlreadyCancelled_whenRegisterAutoCancel_thenPublishAlreadyCancelled() {

        AutoOrderEntity order = mock(AutoOrderEntity.class);

        when(autoOrderService.updateAutoOrderStatusByCancel(1L))
                .thenReturn(new UpdateAutoOrderStatusResult(order, AutoOrderStatus.CANCELED));

        AutoCancelRequestEvent request = new AutoCancelRequestEvent(1L, "005930");

        autoCancelService.registerAutoCancel(request);

        verify(autoCancelResponseEventPublisher)
                .publish(argThat(event ->
                        !event.success() &&
                                event.errorCode() == AutoCancelErrorCode.ALREADY_CANCELLED));
    }

    @Test
    @DisplayName("이미 실행된 AutoOrder에 대해 AutoCancel 요청 시 ALREADY_TRIGGERED 이벤트 발행")
    void givenAlreadyTriggered_whenRegisterAutoCancel_thenPublishAlreadyTriggered() {

        AutoOrderEntity order = mock(AutoOrderEntity.class);

        when(autoOrderService.updateAutoOrderStatusByCancel(1L))
                .thenReturn(new UpdateAutoOrderStatusResult(order, AutoOrderStatus.TRIGGERED));

        AutoCancelRequestEvent request = new AutoCancelRequestEvent(1L, "005930");

        autoCancelService.registerAutoCancel(request);

        verify(autoCancelResponseEventPublisher)
                .publish(argThat(event ->
                        !event.success() &&
                                event.errorCode() == AutoCancelErrorCode.ALREADY_TRIGGERED));
    }

    @Test
    @DisplayName("OPEN 상태의 Buy AutoOrder 취소 시 현금 환불 후 SUCCESS 이벤트 발행")
    void givenOpenBuyAutoOrder_whenRegisterAutoCancel_thenRefundCashAndPublishSuccess() {

        AutoOrderEntity order = mockBuyAutoOrder();

        when(autoOrderService.updateAutoOrderStatusByCancel(1L))
                .thenReturn(new UpdateAutoOrderStatusResult(order, AutoOrderStatus.ACTIVE));

        when(accountBalanceCommand.refundReservedCash(anyString(), anyLong()))
                .thenReturn(true);

        AutoCancelRequestEvent request = new AutoCancelRequestEvent(1L, "005930");

        autoCancelService.registerAutoCancel(request);

        verify(autoOrderQueueRegistry).autoOrderCancel(1L, "005930");
        verify(autoCancelRepository).save(any());
        verify(stockServerAutoOrderResponseRepository).delete("user1", 1L);
        verify(autoCancelResponseEventPublisher)
                .publish(argThat(AutoCancelResponseEvent::success));
        verify(accountUpdateEventPublisher).publish("user1");
    }

    @Test
    @DisplayName("환불 실패 시 INTERNAL_ERROR 이벤트 발행")
    void givenRefundFails_whenRegisterAutoCancel_thenPublishInternalError() {

        AutoOrderEntity order = mockBuyAutoOrder();

        when(autoOrderService.updateAutoOrderStatusByCancel(1L))
                .thenReturn(new UpdateAutoOrderStatusResult(order, AutoOrderStatus.ACTIVE));

        when(accountBalanceCommand.refundReservedCash(anyString(), anyLong()))
                .thenReturn(false);

        AutoCancelRequestEvent request = new AutoCancelRequestEvent(1L, "005930");

        autoCancelService.registerAutoCancel(request);

        verify(autoCancelResponseEventPublisher)
                .publish(argThat(event ->
                        !event.success() &&
                                event.errorCode() == AutoCancelErrorCode.INTERNAL_ERROR));
    }

    @Test
    @DisplayName("OPEN 상태가 아닌 AutoOrder 강제 취소 시 아무 동작도 하지 않음")
    void givenNotOpenAutoOrder_whenForceAutoCancel_thenDoNothing() {

        when(autoOrderService.updateAutoOrderStatusByCancel(1L))
                .thenReturn(new UpdateAutoOrderStatusResult(
                        mock(AutoOrderEntity.class),
                        AutoOrderStatus.CANCELED));

        autoCancelService.forceAutoCancel(1L);

        verifyNoInteractions(autoCancelRepository);
        verifyNoInteractions(accountUpdateEventPublisher);
    }

    @Test
    @DisplayName("OPEN 상태 Sell AutoOrder 강제 취소 시 주식 환불 후 이벤트 발행")
    void givenOpenSellAutoOrder_whenForceAutoCancel_thenRefundStock() {

        AutoOrderEntity order = mockSellAutoOrder();

        UpdateAutoOrderStatusResult result = mock(UpdateAutoOrderStatusResult.class);

        when(autoOrderService.updateAutoOrderStatusByCancel(1L))
                .thenReturn(result);

        when(result.previousStatus()).thenReturn(AutoOrderStatus.ACTIVE);
        when(result.autoOrderEntity()).thenReturn(order);

        when(accountBalanceCommand.refundReservedStock(
                anyString(), anyString(), anyInt()))
                .thenReturn(true);

        autoCancelService.forceAutoCancel(1L);

        verify(autoCancelRepository).save(any());
        verify(stockServerAutoOrderResponseRepository).delete("user1", 1L);
        verify(accountUpdateEventPublisher).publish("user1");
    }

    private AutoOrderEntity mockBuyAutoOrder() {
        AutoOrderEntity order = mock(AutoOrderEntity.class);

        when(order.getAutoOrderId()).thenReturn(1L);
        when(order.getStockCode()).thenReturn("005930");
        when(order.getUsername()).thenReturn("user1");
        when(order.getAutoOrderType()).thenReturn(AutoOrderType.BUY);
        when(order.getOrderPrice()).thenReturn(10000);
        when(order.getOrderQuantity()).thenReturn(10);

        return order;
    }

    private AutoOrderEntity mockSellAutoOrder() {
        AutoOrderEntity order = mock(AutoOrderEntity.class);

        when(order.getAutoOrderId()).thenReturn(1L);
        when(order.getStockCode()).thenReturn("005930");
        when(order.getUsername()).thenReturn("user1");
        when(order.getAutoOrderType()).thenReturn(AutoOrderType.SELL);
        when(order.getOrderQuantity()).thenReturn(10);

        return order;
    }
}

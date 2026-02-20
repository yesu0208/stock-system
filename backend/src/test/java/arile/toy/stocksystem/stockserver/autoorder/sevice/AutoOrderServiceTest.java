package arile.toy.stocksystem.stockserver.autoorder.sevice;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import arile.toy.stocksystem.stockserver.autoorder.dto.*;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.event.StockServerAutoOrderRequestEvent;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.AutoOrderRepository;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AutoOrderServiceTest {

    @Mock
    private AutoOrderRepository autoOrderRepository;

    @Mock
    private AutoOrderQueueRegistry autoOrderQueueRegistry;

    @Mock
    private AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;

    @Mock
    private StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;

    @Mock
    private AccountBalanceCommand accountBalanceCommand;

    @Mock
    private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private AutoOrderService autoOrderService;

    @Test
    @DisplayName("자동 주문 등록 성공 - BUY 타입")
    void givenBuyAutoOrder_whenRegister_thenReserveCashAndEnqueue() {
        // given
        StockServerAutoOrderRequestEvent request = mock(StockServerAutoOrderRequestEvent.class);
        when(request.autoOrderType()).thenReturn(AutoOrderType.BUY);
        when(request.orderPrice()).thenReturn(100);
        when(request.orderQuantity()).thenReturn(2);
        when(request.username()).thenReturn("user1");
        when(request.stockCode()).thenReturn("005930");

        when(accountBalanceCommand.reserveCash("user1", 200L)).thenReturn(true);

        AutoOrderEntity savedEntity = AutoOrderEntity.of(
                "user1", "005930", AutoOrderType.BUY, 90,
                100, 2, AutoOrderStatus.ACTIVE);
        when(autoOrderRepository.save(any())).thenReturn(savedEntity);

        // when
        autoOrderService.registerAutoOrder(request);

        // then
        verify(accountBalanceCommand).reserveCash("user1", 200L);
        verify(autoOrderQueueRegistry).autoOrderEnqueue(any(AutoOrderDto.class));
        verify(stockServerAutoOrderResponseRepository).save(any());
        verify(autoOrderResponseEventPublisher).publish(any());
        verify(accountUpdateEventPublisher).publish("user1");
    }

    @Test
    @DisplayName("자동 주문 등록 실패 - 잔액 부족")
    void givenBuyAutoOrder_whenReserveCashFails_thenPublishError() {
        // given
        StockServerAutoOrderRequestEvent request = mock(StockServerAutoOrderRequestEvent.class);
        when(request.autoOrderType()).thenReturn(AutoOrderType.BUY);
        when(request.orderPrice()).thenReturn(100);
        when(request.orderQuantity()).thenReturn(2);
        when(request.username()).thenReturn("user1");

        when(accountBalanceCommand.reserveCash("user1", 200L)).thenReturn(false);

        // when
        autoOrderService.registerAutoOrder(request);

        // then
        verify(autoOrderResponseEventPublisher).publishError(request, AutoOrderResultCode.INSUFFICIENT_BALANCE);
        verifyNoInteractions(autoOrderQueueRegistry);
        verifyNoInteractions(stockServerAutoOrderResponseRepository);
    }

    @Test
    @DisplayName("자동 주문 상태 취소 - ACTIVE 상태")
    void givenActiveAutoOrder_whenCancel_thenStatusChanged() {
        // given
        AutoOrderEntity entity = AutoOrderEntity.of("user1", "005930",
                AutoOrderType.BUY, 90, 100, 2, AutoOrderStatus.ACTIVE);
        when(autoOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entity));

        // when
        UpdateAutoOrderStatusResult result = autoOrderService.updateAutoOrderStatusByCancel(1L);

        // then
        assertThat(result.previousStatus()).isEqualTo(AutoOrderStatus.ACTIVE);
        assertThat(entity.getAutoOrderStatus()).isEqualTo(AutoOrderStatus.CANCELED);
    }

    @Test
    @DisplayName("자동 주문 상태 취소 - 이미 CANCELED 상태")
    void givenCanceledAutoOrder_whenCancel_thenNoChange() {
        // given
        AutoOrderEntity entity = AutoOrderEntity.of("user1", "005930",
                AutoOrderType.BUY, 90, 100, 2, AutoOrderStatus.CANCELED);
        when(autoOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entity));

        // when
        UpdateAutoOrderStatusResult result = autoOrderService.updateAutoOrderStatusByCancel(1L);

        // then
        assertThat(result.previousStatus()).isEqualTo(AutoOrderStatus.CANCELED);
        assertThat(entity.getAutoOrderStatus()).isEqualTo(AutoOrderStatus.CANCELED);
    }

    @Test
    @DisplayName("모든 미체결 자동 주문 조회")
    void givenAutoOrders_whenFindAllUntriggered_thenReturnList() {
        // given
        AutoOrderEntity entity1 = mock(AutoOrderEntity.class);
        AutoOrderEntity entity2 = mock(AutoOrderEntity.class);
        when(autoOrderRepository.findAllUntriggered()).thenReturn(List.of(entity1, entity2));

        // when
        List<AutoOrderEntity> result = autoOrderService.findAllUntriggeredAutoOrders();

        // then
        assertThat(result).hasSize(2);
    }
}

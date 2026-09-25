package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderService;
import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoService;
import arile.toy.stocksystem.stockserver.otococancel.service.OtocoCancelService;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopService;
import arile.toy.stocksystem.stockserver.trailingstopcancel.service.TrailingStopCancelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 장 마감 미체결 정리 테스트")
@ExtendWith(MockitoExtension.class)
class MarketCloseCleanupServiceTest {

    private static final List<String> STOCK_CODES = List.of("005930", "000660");

    @InjectMocks private MarketCloseCleanupService sut;

    @Mock private OrderService orderService;
    @Mock private CancelService cancelService;
    @Mock private AutoOrderService autoOrderService;
    @Mock private AutoCancelService autoCancelService;
    @Mock private TrailingStopService trailingStopService;
    @Mock private TrailingStopCancelService trailingStopCancelService;
    @Mock private OtocoService otocoService;
    @Mock private OtocoCancelService otocoCancelService;

    @DisplayName("미체결 주문·자동주문·트레일링 스탑·OTOCO를 모두 강제 취소하고 실패 0건을 반환한다")
    @Test
    void givenTargets_whenCleaningUp_thenForceCancelsAll() {
        // Given
        givenOrders(1L, 2L);
        givenAutoOrders(11L);
        givenTrailingStops(21L);
        givenOtocos(31L);

        // When
        int failures = sut.cleanUp(STOCK_CODES);

        // Then
        assertThat(failures).isZero();
        then(cancelService).should().forceCancel(1L);
        then(cancelService).should().forceCancel(2L);
        then(autoCancelService).should().forceAutoCancel(11L);
        then(trailingStopCancelService).should().forceCancelTrailingStop(21L);
        then(otocoCancelService).should().forceCancelOtoco(31L);
    }

    @DisplayName("정리 대상이 없으면 취소를 호출하지 않는다")
    @Test
    void givenNoTargets_whenCleaningUp_thenCancelsNothing() {
        givenOrders();
        givenAutoOrders();
        givenTrailingStops();
        givenOtocos();

        int failures = sut.cleanUp(STOCK_CODES);

        assertThat(failures).isZero();
        then(cancelService).shouldHaveNoInteractions();
        then(autoCancelService).shouldHaveNoInteractions();
        then(trailingStopCancelService).shouldHaveNoInteractions();
        then(otocoCancelService).shouldHaveNoInteractions();
    }

    @DisplayName("한 건의 취소가 실패해도 같은 종류의 나머지와 다른 종류의 정리를 계속한다")
    @Test
    void givenOneCancelFails_whenCleaningUp_thenContinuesAndCountsFailure() {
        givenOrders(1L, 2L);
        givenAutoOrders(11L);
        givenTrailingStops(21L);
        givenOtocos(31L);
        willThrow(new IllegalStateException("fail")).given(cancelService).forceCancel(1L);

        int failures = sut.cleanUp(STOCK_CODES);

        assertThat(failures).isEqualTo(1);
        then(cancelService).should().forceCancel(2L);
        then(autoCancelService).should().forceAutoCancel(11L);
        then(trailingStopCancelService).should().forceCancelTrailingStop(21L);
        then(otocoCancelService).should().forceCancelOtoco(31L);
    }

    @DisplayName("한 종류의 조회가 실패해도 다른 종류의 정리를 계속한다")
    @Test
    void givenFindFails_whenCleaningUp_thenContinuesOtherKinds() {
        given(orderService.findAllUnfilledOrders(STOCK_CODES)).willThrow(new IllegalStateException("db error"));
        givenAutoOrders(11L);
        givenTrailingStops(21L);
        givenOtocos(31L);

        int failures = sut.cleanUp(STOCK_CODES);

        assertThat(failures).isEqualTo(1);
        then(cancelService).shouldHaveNoInteractions();
        then(autoCancelService).should().forceAutoCancel(11L);
        then(trailingStopCancelService).should().forceCancelTrailingStop(21L);
        then(otocoCancelService).should().forceCancelOtoco(31L);
    }

    // ===== helpers =====

    private void givenOrders(Long... ids) {
        List<OrderEntity> orders = java.util.Arrays.stream(ids).map(id -> {
            OrderEntity entity = new OrderEntity();
            entity.setOrderId(id);
            return entity;
        }).toList();
        given(orderService.findAllUnfilledOrders(STOCK_CODES)).willReturn(orders);
    }

    private void givenAutoOrders(Long... ids) {
        List<AutoOrderEntity> entities = java.util.Arrays.stream(ids).map(id -> {
            AutoOrderEntity entity = mock(AutoOrderEntity.class);
            given(entity.getAutoOrderId()).willReturn(id);
            return entity;
        }).toList();
        given(autoOrderService.findAllUntriggeredAutoOrders(STOCK_CODES)).willReturn(entities);
    }

    private void givenTrailingStops(Long... ids) {
        List<TrailingStopEntity> entities = java.util.Arrays.stream(ids).map(id -> {
            TrailingStopEntity entity = mock(TrailingStopEntity.class);
            given(entity.getTrailingStopId()).willReturn(id);
            return entity;
        }).toList();
        given(trailingStopService.findAllUntriggeredTrailingStops(STOCK_CODES)).willReturn(entities);
    }

    private void givenOtocos(Long... ids) {
        List<OtocoEntity> entities = java.util.Arrays.stream(ids).map(id -> {
            OtocoEntity entity = mock(OtocoEntity.class);
            given(entity.getOtocoId()).willReturn(id);
            return entity;
        }).toList();
        given(otocoService.findAllUnfinishedOtocos(STOCK_CODES)).willReturn(entities);
    }
}

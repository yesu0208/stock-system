package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoLeg;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] OTOCO 청산 발동·주문 등록 테스트")
@ExtendWith(MockitoExtension.class)
class OtocoExitTransactionalServiceTest {

    @InjectMocks private OtocoExitTransactionalService sut;

    @Mock private OtocoRepository otocoRepository;
    @Mock private OrderService orderService;
    @Mock private StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    @Mock private OtocoResponseEventPublisher otocoResponseEventPublisher;

    @DisplayName("WAITING_EXIT가 아니면 아무것도 하지 않는다")
    @Test
    void givenNotWaitingExit_whenTriggering_thenNothing() {
        givenEntity(OtocoStatus.COMPLETED);

        sut.triggerExit(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT), OtocoLeg.TAKE_PROFIT);

        then(orderService).shouldHaveNoInteractions();
    }

    @DisplayName("익절은 익절가, 손절은 손절가로 일반 매도 주문을 등록해 주식을 예약하고 COMPLETED로 저장한다")
    @Test
    void givenWaitingExit_whenTriggering_thenRegistersSellWithReservation() {
        OtocoEntity entity = givenEntity(OtocoStatus.WAITING_EXIT);
        given(orderService.registerOrder(any(), eq(false))).willReturn(mock(OrderEntity.class));

        sut.triggerExit(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT), OtocoLeg.TAKE_PROFIT);

        then(orderService).should().registerOrder(argThat(e ->
                e.orderType() == OrderType.SELL && e.orderPrice() == 73_500 && e.orderQuantity() == 10), eq(false));
        assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.COMPLETED);
        assertThat(entity.getCompletedLeg()).isEqualTo(OtocoLeg.TAKE_PROFIT);
        then(otocoRepository).should().save(entity);
        then(stockServerOtocoResponseRepository).should().delete("user", 1L);
        then(otocoResponseEventPublisher).should().publishExitTriggered(any(), eq(OtocoLeg.TAKE_PROFIT));
    }

    @DisplayName("손절은 손절가로 주문한다")
    @Test
    void givenStopLoss_whenTriggering_thenUsesSlPrice() {
        givenEntity(OtocoStatus.WAITING_EXIT);
        given(orderService.registerOrder(any(), eq(false))).willReturn(mock(OrderEntity.class));

        sut.triggerExit(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT), OtocoLeg.STOP_LOSS);

        then(orderService).should().registerOrder(argThat(e -> e.orderPrice() == 67_900), eq(false));
    }

    @DisplayName("주문 등록 후 응답 삭제가 실패해도 청산 이벤트는 발행한다")
    @Test
    void givenDeleteFails_whenTriggering_thenStillPublishes() {
        givenEntity(OtocoStatus.WAITING_EXIT);
        given(orderService.registerOrder(any(), eq(false))).willReturn(mock(OrderEntity.class));
        willThrow(new IllegalStateException("redis down")).given(stockServerOtocoResponseRepository).delete("user", 1L);

        sut.triggerExit(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT), OtocoLeg.STOP_LOSS);

        then(otocoResponseEventPublisher).should().publishExitTriggered(any(), eq(OtocoLeg.STOP_LOSS));
    }

    @DisplayName("주식 예약 실패로 주문이 등록되지 않으면 CANCELED로 저장하고 청산 실패를 발행한다")
    @Test
    void givenOrderNull_whenTriggering_thenCancels() {
        OtocoEntity entity = givenEntity(OtocoStatus.WAITING_EXIT);
        willThrow(new IllegalStateException("redis down")).given(stockServerOtocoResponseRepository).delete("user", 1L);

        sut.triggerExit(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT), OtocoLeg.TAKE_PROFIT);

        assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.CANCELED);
        then(otocoRepository).should().save(entity);
        then(otocoResponseEventPublisher).should().publishExitFailed(any(), eq(OtocoResultCode.INTERNAL_ERROR));
    }

    @DisplayName("주문 등록 중 예외는 그대로 전파한다 (롤백 후 재시도)")
    @Test
    void givenOrderThrows_whenTriggering_thenPropagates() {
        OtocoEntity entity = givenEntity(OtocoStatus.WAITING_EXIT);
        given(orderService.registerOrder(any(), eq(false))).willThrow(new IllegalStateException("order error"));

        assertThatThrownBy(() -> sut.triggerExit(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT), OtocoLeg.TAKE_PROFIT))
                .hasMessage("order error");

        assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.WAITING_EXIT);
        then(otocoResponseEventPublisher).shouldHaveNoInteractions();
    }

    @DisplayName("OTOCO가 없으면 예외를 던지고 주문을 등록하지 않는다")
    @Test
    void givenNotFound_whenTriggering_thenThrows() {
        given(otocoRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.triggerExit(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT), OtocoLeg.TAKE_PROFIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("otoco not found");

        then(orderService).shouldHaveNoInteractions();
        then(otocoResponseEventPublisher).shouldHaveNoInteractions();
    }

    @DisplayName("주문이 등록되지 않으면 응답을 삭제하고 CANCELED로 저장한 뒤 청산 실패를 발행한다")
    @Test
    void givenOrderNull_whenTriggering_thenDeletesResponseAndCancels() {
        OtocoEntity entity = givenEntity(OtocoStatus.WAITING_EXIT);

        sut.triggerExit(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT), OtocoLeg.STOP_LOSS);

        assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.CANCELED);
        then(otocoRepository).should().save(entity);
        then(stockServerOtocoResponseRepository).should().delete("user", 1L);
        then(otocoResponseEventPublisher).should().publishExitFailed(any(), eq(OtocoResultCode.INTERNAL_ERROR));
        then(otocoResponseEventPublisher).should(never()).publishExitTriggered(any(), any());
    }

    private OtocoEntity givenEntity(OtocoStatus status) {
        OtocoEntity entity = OtocoFixtures.entity(status);
        given(otocoRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
        return entity;
    }
}

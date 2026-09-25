package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] OTOCO 진입 발동·주문 등록 테스트")
@ExtendWith(MockitoExtension.class)
class OtocoEntryTransactionalServiceTest {

    @InjectMocks private OtocoEntryTransactionalService sut;

    @Mock private OtocoRepository otocoRepository;
    @Mock private OrderService orderService;
    @Mock private StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    @Mock private OtocoResponseEventPublisher otocoResponseEventPublisher;
    @Mock private AccountApiClient accountApiClient;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @DisplayName("WAITING_ENTRY가 아니면 아무것도 하지 않는다")
    @Test
    void givenNotWaiting_whenTriggering_thenNothing() {
        givenEntity(OtocoStatus.CANCELED);

        sut.triggerEntryAndRegisterOrder(OtocoFixtures.dto(OtocoStatus.WAITING_ENTRY));

        then(orderService).shouldHaveNoInteractions();
    }

    @DisplayName("OTOCO가 없으면 예외를 던진다")
    @Test
    void givenNotFound_whenTriggering_thenThrows() {
        given(otocoRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.triggerEntryAndRegisterOrder(OtocoFixtures.dto(OtocoStatus.WAITING_ENTRY)))
                .hasMessage("otoco not found");
    }

    @DisplayName("예약된 매수 주문으로 등록하고 주문 id·ENTRY_ORDER_PLACED를 저장한 뒤 응답 갱신·발행한다")
    @Test
    void givenWaiting_whenTriggering_thenRegistersOrder() {
        OtocoEntity entity = givenEntity(OtocoStatus.WAITING_ENTRY);
        givenOrderRegistered();

        sut.triggerEntryAndRegisterOrder(OtocoFixtures.dto(OtocoStatus.WAITING_ENTRY));

        then(orderService).should().registerOrder(argThat(e -> e.orderPrice() == 70_000 && e.orderQuantity() == 10), eq(true));
        assertThat(entity.getEntryOrderId()).isEqualTo(100L);
        assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.ENTRY_ORDER_PLACED);
        then(otocoRepository).should().save(entity);
        then(stockServerOtocoResponseRepository).should().update(eq("user"), eq(1L), any());
        then(otocoResponseEventPublisher).should().publishEntryTriggered(any());
        then(accountApiClient).shouldHaveNoInteractions();
    }

    @DisplayName("주문 등록 후 응답 갱신이 실패해도 예외 없이 발행한다")
    @Test
    void givenResponseUpdateFails_whenTriggering_thenStillPublishes() {
        givenEntity(OtocoStatus.WAITING_ENTRY);
        givenOrderRegistered();
        willThrow(new IllegalStateException("redis down"))
                .given(stockServerOtocoResponseRepository).update(eq("user"), eq(1L), any());

        sut.triggerEntryAndRegisterOrder(OtocoFixtures.dto(OtocoStatus.WAITING_ENTRY));

        then(otocoResponseEventPublisher).should().publishEntryTriggered(any());
    }

    @DisplayName("주문 등록 중 예외가 나면 환불하지 않고 그대로 전파한다 (롤백 후 재시도, 예약 유지)")
    @Test
    void givenOrderThrows_whenTriggering_thenPropagatesWithoutRefund() {
        OtocoEntity entity = givenEntity(OtocoStatus.WAITING_ENTRY);
        given(orderService.registerOrder(any(), eq(true))).willThrow(new IllegalStateException("order error"));

        assertThatThrownBy(() -> sut.triggerEntryAndRegisterOrder(OtocoFixtures.dto(OtocoStatus.WAITING_ENTRY)))
                .hasMessage("order error");

        then(accountApiClient).shouldHaveNoInteractions();
        then(otocoRepository).should(never()).save(any());
        assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.WAITING_ENTRY);
    }

    @DisplayName("주문이 등록되지 않으면 CANCELED 저장(flush) 후 예약액을 환불하고 실패를 발행한다")
    @Test
    void givenOrderNull_whenTriggering_thenCompensatesInOrder() {
        OtocoEntity entity = givenEntity(OtocoStatus.WAITING_ENTRY, LeverageRatio.X2);
        given(accountApiClient.refundReservedCash("user", 350_105L)).willReturn(true);

        sut.triggerEntryAndRegisterOrder(OtocoFixtures.dto(OtocoStatus.WAITING_ENTRY));

        InOrder inOrder = inOrder(otocoRepository, accountApiClient, stockServerOtocoResponseRepository, otocoResponseEventPublisher);
        inOrder.verify(otocoRepository).saveAndFlush(entity);
        inOrder.verify(accountApiClient).refundReservedCash("user", 350_105L);
        inOrder.verify(stockServerOtocoResponseRepository).delete("user", 1L);
        inOrder.verify(otocoResponseEventPublisher).publishEntryFailed(any(), eq(OtocoResultCode.ENTRY_FAILED));
        assertThat(entity.getOtocoStatus()).isEqualTo(OtocoStatus.CANCELED);
    }

    @DisplayName("보상 중 환불·응답 삭제가 실패해도 실패 이벤트는 발행한다")
    @Test
    void givenCompensationPartsFail_whenTriggering_thenStillPublishesFailure() {
        givenEntity(OtocoStatus.WAITING_ENTRY);
        given(accountApiClient.refundReservedCash("user", 700_105L)).willReturn(false);
        willThrow(new IllegalStateException("redis down")).given(stockServerOtocoResponseRepository).delete("user", 1L);

        sut.triggerEntryAndRegisterOrder(OtocoFixtures.dto(OtocoStatus.WAITING_ENTRY));

        then(otocoResponseEventPublisher).should().publishEntryFailed(any(), eq(OtocoResultCode.ENTRY_FAILED));
    }

    private OtocoEntity givenEntity(OtocoStatus status) {
        return givenEntity(status, LeverageRatio.SPOT);
    }

    private OtocoEntity givenEntity(OtocoStatus status, LeverageRatio ratio) {
        OtocoEntity entity = OtocoFixtures.entity(1L, status, ratio);
        given(otocoRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
        return entity;
    }

    private void givenOrderRegistered() {
        OrderEntity order = mock(OrderEntity.class);
        given(order.getOrderId()).willReturn(100L);
        given(orderService.registerOrder(any(), eq(true))).willReturn(order);
    }
}

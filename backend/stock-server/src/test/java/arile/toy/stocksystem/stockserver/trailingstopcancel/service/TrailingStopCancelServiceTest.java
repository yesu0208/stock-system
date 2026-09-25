package arile.toy.stocksystem.stockserver.trailingstopcancel.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.dto.UpdateTrailingStopStatusResult;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.registry.TrailingStopBookRegistry;
import arile.toy.stocksystem.stockserver.trailingstop.repository.StockServerTrailingStopResponseRepository;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopService;
import arile.toy.stocksystem.stockserver.trailingstopcancel.dto.TrailingStopCancelErrorCode;
import arile.toy.stocksystem.stockserver.trailingstopcancel.entity.TrailingStopCancelEntity;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.TrailingStopCancelResponseEvent;
import arile.toy.stocksystem.stockserver.trailingstopcancel.event.publisher.TrailingStopCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trailingstopcancel.repository.TrailingStopCancelRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

@DisplayName("[Service] 트레일링 스탑 취소 테스트")
@ExtendWith(MockitoExtension.class)
class TrailingStopCancelServiceTest {

    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";

    @InjectMocks private TrailingStopCancelService sut;

    @Mock private TrailingStopService trailingStopService;
    @Mock private TrailingStopCancelRepository trailingStopCancelRepository;
    @Mock private TrailingStopBookRegistry trailingStopBookRegistry;
    @Mock private TrailingStopCancelResponseEventPublisher trailingStopCancelResponseEventPublisher;
    @Mock private StockServerTrailingStopResponseRepository stockServerTrailingStopResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @Nested
    @DisplayName("registerCancel (사용자 취소)")
    class RegisterCancel {

        @DisplayName("소유자·종목이 맞지 않으면 아무것도 하지 않는다")
        @Test
        void givenNotOwner_whenCanceling_thenNothing() {
            given(trailingStopService.updateTrailingStopStatusByUserCancel(1L, USERNAME, STOCK_CODE))
                    .willReturn(Optional.empty());

            sut.registerCancel(request());

            then(trailingStopCancelRepository).shouldHaveNoInteractions();
            then(accountApiClient).shouldHaveNoInteractions();
            then(trailingStopCancelResponseEventPublisher).shouldHaveNoInteractions();
        }

        @DisplayName("이미 취소·발동된 경우 각각의 에러 응답만 발행한다")
        @Test
        void givenClosed_whenCanceling_thenPublishesError() {
            TrailingStopEntity entity = entity(TrailingStopType.SELL, LeverageRatio.SPOT);
            given(trailingStopService.updateTrailingStopStatusByUserCancel(1L, USERNAME, STOCK_CODE))
                    .willReturn(Optional.of(UpdateTrailingStopStatusResult.of(entity, TrailingStopStatus.CANCELED)))
                    .willReturn(Optional.of(UpdateTrailingStopStatusResult.of(entity, TrailingStopStatus.TRIGGERED)));

            sut.registerCancel(request());
            sut.registerCancel(request());

            ArgumentCaptor<TrailingStopCancelResponseEvent> captor = ArgumentCaptor.forClass(TrailingStopCancelResponseEvent.class);
            then(trailingStopCancelResponseEventPublisher).should(times(2)).publish(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(TrailingStopCancelResponseEvent::errorCode)
                    .containsExactly(TrailingStopCancelErrorCode.ALREADY_CANCELLED, TrailingStopCancelErrorCode.ALREADY_TRIGGERED);
            then(accountApiClient).shouldHaveNoInteractions();
            then(trailingStopCancelRepository).shouldHaveNoInteractions();
        }

        @DisplayName("매수 취소: 이력 저장 → 등록 시점 발동가 기준 예약액 환불 → 북 제거 → 응답 삭제·성공 발행 순으로 처리한다")
        @Test
        void givenActiveBuy_whenCanceling_thenSavesRefundsRemoves() {
            // 추적으로 현재 발동가가 70,100이어도 환불은 등록 시점 72,100 기준: 721,000 + 108
            TrailingStopEntity entity = entity(TrailingStopType.BUY, LeverageRatio.SPOT);
            entity.setCurrentTriggerPrice(70_100);
            givenActive(entity);
            given(accountApiClient.refundReservedCash(USERNAME, 721_108L)).willReturn(true);

            sut.registerCancel(request());

            InOrder inOrder = inOrder(trailingStopCancelRepository, accountApiClient, trailingStopBookRegistry,
                    stockServerTrailingStopResponseRepository, trailingStopCancelResponseEventPublisher);
            inOrder.verify(trailingStopCancelRepository).saveAndFlush(argThat(c -> c.getTrailingStopId().equals(1L)));
            inOrder.verify(accountApiClient).refundReservedCash(USERNAME, 721_108L);
            inOrder.verify(trailingStopBookRegistry).remove(STOCK_CODE, 1L);
            inOrder.verify(stockServerTrailingStopResponseRepository).delete(USERNAME, 1L);
            inOrder.verify(trailingStopCancelResponseEventPublisher).publish(argThat(e ->
                    e.success() && e.errorCode() == null && e.triggerPrice() == 70_100));
        }

        @DisplayName("레버리지 매수 취소: 증거금+수수료를 환불한다")
        @Test
        void givenLeverageBuy_whenCanceling_thenRefundsMargin() {
            givenActive(entity(TrailingStopType.BUY, LeverageRatio.X2));
            given(accountApiClient.refundReservedCash(USERNAME, 360_608L)).willReturn(true);

            sut.registerCancel(request());

            then(accountApiClient).should().refundReservedCash(USERNAME, 360_608L);
        }

        @DisplayName("매도 취소: 현물은 주식을, 레버리지는 포지션을 환불한다")
        @Test
        void givenSell_whenCanceling_thenRefundsStock() {
            given(trailingStopService.updateTrailingStopStatusByUserCancel(1L, USERNAME, STOCK_CODE))
                    .willReturn(Optional.of(UpdateTrailingStopStatusResult.of(
                            entity(TrailingStopType.SELL, LeverageRatio.SPOT), TrailingStopStatus.ACTIVE)))
                    .willReturn(Optional.of(UpdateTrailingStopStatusResult.of(
                            entity(TrailingStopType.SELL, LeverageRatio.X2), TrailingStopStatus.ACTIVE)));
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            given(accountApiClient.refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10)).willReturn(true);

            sut.registerCancel(request());
            sut.registerCancel(request());

            then(accountApiClient).should().refundReservedStock(USERNAME, STOCK_CODE, 10);
            then(accountApiClient).should().refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10);
        }

        @DisplayName("이력 저장이 실패하면 환불하지 않고 에러 발행 후 예외를 다시 던진다 (롤백 후 재시도)")
        @Test
        void givenSaveFails_whenCanceling_thenNoRefundAndRethrows() {
            givenActive(entity(TrailingStopType.BUY, LeverageRatio.SPOT));
            given(trailingStopCancelRepository.saveAndFlush(any())).willThrow(new IllegalStateException("db error"));

            assertThatThrownBy(() -> sut.registerCancel(request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("db error");

            then(accountApiClient).shouldHaveNoInteractions();
            then(trailingStopBookRegistry).shouldHaveNoInteractions();
            then(trailingStopCancelResponseEventPublisher).should().publish(argThat(e ->
                    !e.success() && e.errorCode() == TrailingStopCancelErrorCode.INTERNAL_ERROR));
        }

        @DisplayName("환불이 실패하면 북에서 제거하지 않고 에러 발행 후 예외를 다시 던진다")
        @Test
        void givenRefundFails_whenCanceling_thenRethrows() {
            givenActive(entity(TrailingStopType.SELL, LeverageRatio.SPOT));
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(false);

            assertThatThrownBy(() -> sut.registerCancel(request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Stock refund failed");

            then(trailingStopBookRegistry).shouldHaveNoInteractions();
            then(stockServerTrailingStopResponseRepository).shouldHaveNoInteractions();
            then(trailingStopCancelResponseEventPublisher).should().publish(argThat(e ->
                    e.errorCode() == TrailingStopCancelErrorCode.INTERNAL_ERROR));
        }

        @DisplayName("현금 환불 실패도 예외를 다시 던진다")
        @Test
        void givenCashRefundFails_whenCanceling_thenRethrows() {
            givenActive(entity(TrailingStopType.BUY, LeverageRatio.SPOT));
            given(accountApiClient.refundReservedCash(USERNAME, 721_108L)).willReturn(false);

            assertThatThrownBy(() -> sut.registerCancel(request()))
                    .hasMessage("Cash refund failed");
        }

        @DisplayName("환불 이후 북 제거·응답 삭제가 실패해도 예외 없이 성공을 발행한다 (이중 환불 방지)")
        @Test
        void givenPostRefundFails_whenCanceling_thenStillSucceeds() {
            givenActive(entity(TrailingStopType.SELL, LeverageRatio.SPOT));
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            willThrow(new IllegalStateException("book error")).given(trailingStopBookRegistry).remove(STOCK_CODE, 1L);
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerTrailingStopResponseRepository).delete(USERNAME, 1L);

            sut.registerCancel(request());

            then(accountApiClient).should(times(1)).refundReservedStock(USERNAME, STOCK_CODE, 10);
            then(trailingStopCancelResponseEventPublisher).should().publish(argThat(TrailingStopCancelResponseEvent::success));
        }
    }

    @Nested
    @DisplayName("forceCancelTrailingStop (시스템 강제 취소)")
    class ForceCancel {

        @DisplayName("이미 종료된 트레일링 스탑이면 아무것도 하지 않는다")
        @Test
        void givenClosed_whenForceCanceling_thenNothing() {
            given(trailingStopService.updateTrailingStopStatusByCancel(1L)).willReturn(
                    UpdateTrailingStopStatusResult.of(entity(TrailingStopType.SELL, LeverageRatio.SPOT), TrailingStopStatus.TRIGGERED));

            sut.forceCancelTrailingStop(1L);

            then(trailingStopCancelRepository).shouldHaveNoInteractions();
            then(accountApiClient).shouldHaveNoInteractions();
        }

        @DisplayName("ACTIVE면 이력 저장·환불·북 제거 후 성공을 발행한다")
        @Test
        void givenActive_whenForceCanceling_thenCancels() {
            given(trailingStopService.updateTrailingStopStatusByCancel(1L)).willReturn(
                    UpdateTrailingStopStatusResult.of(entity(TrailingStopType.SELL, LeverageRatio.SPOT), TrailingStopStatus.ACTIVE));
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(true);

            sut.forceCancelTrailingStop(1L);

            then(trailingStopCancelRepository).should().saveAndFlush(any(TrailingStopCancelEntity.class));
            then(trailingStopBookRegistry).should().remove(STOCK_CODE, 1L);
            then(trailingStopCancelResponseEventPublisher).should().publish(argThat(TrailingStopCancelResponseEvent::success));
        }

        @DisplayName("환불이 실패해도 예외를 던지지 않는다 (장 마감 정산에서 예약 해제)")
        @Test
        void givenRefundFails_whenForceCanceling_thenSwallows() {
            given(trailingStopService.updateTrailingStopStatusByCancel(1L)).willReturn(
                    UpdateTrailingStopStatusResult.of(entity(TrailingStopType.SELL, LeverageRatio.SPOT), TrailingStopStatus.ACTIVE));
            given(accountApiClient.refundReservedStock(USERNAME, STOCK_CODE, 10)).willReturn(false);

            sut.forceCancelTrailingStop(1L);

            then(trailingStopCancelResponseEventPublisher).shouldHaveNoInteractions();
        }
    }

    // ===== helpers =====

    private TrailingStopCancelRequestEvent request() {
        return TrailingStopCancelRequestEvent.of(1L, STOCK_CODE, USERNAME);
    }

    private void givenActive(TrailingStopEntity entity) {
        given(trailingStopService.updateTrailingStopStatusByUserCancel(1L, USERNAME, STOCK_CODE))
                .willReturn(Optional.of(UpdateTrailingStopStatusResult.of(entity, TrailingStopStatus.ACTIVE)));
    }

    private TrailingStopEntity entity(TrailingStopType type, LeverageRatio ratio) {
        int trigger = type == TrailingStopType.BUY ? 72_100 : 67_900;
        TrailingStopEntity entity = TrailingStopEntity.of(USERNAME, STOCK_CODE, type, ratio,
                10, 3.0, 70_000, trigger, TrailingStopStatus.CANCELED);
        entity.setTrailingStopId(1L);
        return entity;
    }
}

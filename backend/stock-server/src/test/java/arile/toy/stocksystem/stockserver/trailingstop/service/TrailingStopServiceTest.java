package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.trailingstop.dto.*;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.event.StockServerTrailingStopRequestEvent;
import arile.toy.stocksystem.stockserver.trailingstop.event.publisher.TrailingStopResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trailingstop.registry.TrailingStopBookRegistry;
import arile.toy.stocksystem.stockserver.trailingstop.repository.StockServerTrailingStopResponseRepository;
import arile.toy.stocksystem.stockserver.trailingstop.repository.TrailingStopRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 트레일링 스탑 등록·상태 변경 테스트")
@ExtendWith(MockitoExtension.class)
class TrailingStopServiceTest {

    private static final String USERNAME = "user";
    private static final String STOCK_CODE = "005930";
    // 매수: 기준가 70,000, 3% → 초기 발동가 72,100 × 10주 = 721,000 → 수수료 108
    private static final long BUY_RESERVE = 721_108L;

    @InjectMocks private TrailingStopService sut;

    @Mock private TrailingStopRepository trailingStopRepository;
    @Mock private TrailingStopBookRegistry trailingStopBookRegistry;
    @Mock private TrailingStopResponseEventPublisher trailingStopResponseEventPublisher;
    @Mock private StockServerTrailingStopResponseRepository stockServerTrailingStopResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @Nested
    @DisplayName("registerTrailingStop")
    class Register {

        @DisplayName("매수: 초기 발동가 기준 금액+수수료를 예약하고, ACTIVE로 저장·북 등록·응답 발행한다")
        @Test
        void givenBuy_whenRegistering_thenReservesAtInitialTrigger() {
            given(accountApiClient.reserveCash(USERNAME, BUY_RESERVE)).willReturn(true);
            givenSaveAssignsId();

            sut.registerTrailingStop(request(TrailingStopType.BUY, LeverageRatio.SPOT));

            ArgumentCaptor<TrailingStopEntity> captor = ArgumentCaptor.forClass(TrailingStopEntity.class);
            then(trailingStopRepository).should().save(captor.capture());
            TrailingStopEntity saved = captor.getValue();
            assertThat(saved.getBasePrice()).isEqualTo(70_000);
            assertThat(saved.getTriggerPrice()).isEqualTo(72_100);
            assertThat(saved.getCurrentBasePrice()).isNull();
            assertThat(saved.getCurrentTriggerPrice()).isNull();
            assertThat(saved.getTrailingStopStatus()).isEqualTo(TrailingStopStatus.ACTIVE);

            then(trailingStopBookRegistry).should().register(argThat(d ->
                    d.trailingStopId().equals(1L) && d.triggerPrice() == 72_100 && d.initialTriggerPrice() == 72_100));
            then(stockServerTrailingStopResponseRepository).should().save(any());
            then(trailingStopResponseEventPublisher).should().publish(any());
        }

        @DisplayName("레버리지 매수: 초기 발동가 기준 증거금+수수료를 예약한다")
        @Test
        void givenLeverageBuy_whenRegistering_thenReservesMarginPlusFee() {
            // 721,000 × 0.5 = 360,500 + 수수료 108
            given(accountApiClient.reserveCash(USERNAME, 360_608L)).willReturn(true);
            givenSaveAssignsId();

            sut.registerTrailingStop(request(TrailingStopType.BUY, LeverageRatio.X2));

            then(accountApiClient).should().reserveCash(USERNAME, 360_608L);
        }

        @DisplayName("레버리지 비율이 없으면 현물로 저장한다")
        @Test
        void givenNullLeverage_whenRegistering_thenSpot() {
            given(accountApiClient.reserveCash(USERNAME, BUY_RESERVE)).willReturn(true);
            givenSaveAssignsId();

            sut.registerTrailingStop(request(TrailingStopType.BUY, null));

            then(trailingStopRepository).should().save(argThat(e -> e.getLeverageRatio() == LeverageRatio.SPOT));
        }

        @DisplayName("매도: 현물은 주식을, 레버리지는 포지션을 예약하고 현금은 예약하지 않는다")
        @Test
        void givenSell_whenRegistering_thenReservesStockOrLeverage() {
            given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            given(accountApiClient.reserveLeverageStock(USERNAME, STOCK_CODE, "X2", 10)).willReturn(true);
            givenSaveAssignsId();

            sut.registerTrailingStop(request(TrailingStopType.SELL, LeverageRatio.SPOT));
            sut.registerTrailingStop(request(TrailingStopType.SELL, LeverageRatio.X2));

            then(accountApiClient).should().reserveStock(USERNAME, STOCK_CODE, 10);
            then(accountApiClient).should().reserveLeverageStock(USERNAME, STOCK_CODE, "X2", 10);
            then(accountApiClient).should(never()).reserveCash(anyString(), anyLong());
        }

        @DisplayName("예약에 실패하면 매수는 잔고 부족, 매도는 보유 주식 부족 에러를 발행하고 저장하지 않는다")
        @Test
        void givenReserveFails_whenRegistering_thenPublishesError() {
            var buy = request(TrailingStopType.BUY, LeverageRatio.SPOT);
            var sell = request(TrailingStopType.SELL, LeverageRatio.SPOT);
            given(accountApiClient.reserveCash(USERNAME, BUY_RESERVE)).willReturn(false);
            given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, 10)).willReturn(false);

            sut.registerTrailingStop(buy);
            sut.registerTrailingStop(sell);

            then(trailingStopResponseEventPublisher).should().publishError(buy, TrailingStopResultCode.INSUFFICIENT_BALANCE);
            then(trailingStopResponseEventPublisher).should().publishError(sell, TrailingStopResultCode.INSUFFICIENT_STOCK);
            then(trailingStopRepository).shouldHaveNoInteractions();
        }

        @DisplayName("저장 후 북 등록이 실패하면 북에서 제거·CANCELED로 무효화하고 환불·에러 발행 후 예외를 다시 던진다")
        @Test
        void givenBookRegisterFails_whenRegistering_thenCancelsAndRefunds() {
            var request = request(TrailingStopType.BUY, LeverageRatio.SPOT);
            given(accountApiClient.reserveCash(USERNAME, BUY_RESERVE)).willReturn(true);
            givenSaveAssignsId();
            willThrow(new IllegalStateException("book error")).given(trailingStopBookRegistry).register(any());

            assertThatThrownBy(() -> sut.registerTrailingStop(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("book error");

            ArgumentCaptor<TrailingStopEntity> captor = ArgumentCaptor.forClass(TrailingStopEntity.class);
            then(trailingStopRepository).should(times(2)).save(captor.capture());
            assertThat(captor.getValue().getTrailingStopStatus()).isEqualTo(TrailingStopStatus.CANCELED);
            then(trailingStopBookRegistry).should().remove(STOCK_CODE, 1L);
            then(accountApiClient).should().refundReservedCash(USERNAME, BUY_RESERVE);
            then(trailingStopResponseEventPublisher).should().publishError(request, TrailingStopResultCode.INTERNAL_ERROR);
            then(stockServerTrailingStopResponseRepository).shouldHaveNoInteractions();
        }

        @DisplayName("무효화 저장까지 실패해도 원래 예외에 첨부하고 환불은 진행한다")
        @Test
        void givenCancelSaveAlsoFails_whenRegistering_thenAttachesSuppressedAndRefunds() {
            given(accountApiClient.reserveCash(USERNAME, BUY_RESERVE)).willReturn(true);
            given(trailingStopRepository.save(any(TrailingStopEntity.class)))
                    .willAnswer(invocation -> {
                        TrailingStopEntity entity = invocation.getArgument(0);
                        entity.setTrailingStopId(1L);
                        return entity;
                    })
                    .willThrow(new IllegalStateException("cancel save error"));
            willThrow(new IllegalStateException("book error")).given(trailingStopBookRegistry).register(any());

            assertThatThrownBy(() -> sut.registerTrailingStop(request(TrailingStopType.BUY, LeverageRatio.SPOT)))
                    .hasMessage("book error")
                    .satisfies(e -> assertThat(e.getSuppressed()).singleElement()
                            .extracting(Throwable::getMessage).isEqualTo("cancel save error"));

            then(accountApiClient).should().refundReservedCash(USERNAME, BUY_RESERVE);
        }

        @DisplayName("저장 자체가 실패하면 무효화 없이 예약분만 환불한다 (현물 매도 → 주식, 레버리지 매도 → 포지션)")
        @Test
        void givenSaveFails_whenRegistering_thenRefundsOnly() {
            given(accountApiClient.reserveStock(USERNAME, STOCK_CODE, 10)).willReturn(true);
            given(accountApiClient.reserveLeverageStock(USERNAME, STOCK_CODE, "X2", 10)).willReturn(true);
            given(trailingStopRepository.save(any())).willThrow(new IllegalStateException("db error"));

            assertThatThrownBy(() -> sut.registerTrailingStop(request(TrailingStopType.SELL, LeverageRatio.SPOT)))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> sut.registerTrailingStop(request(TrailingStopType.SELL, LeverageRatio.X2)))
                    .isInstanceOf(IllegalStateException.class);

            then(trailingStopBookRegistry).shouldHaveNoInteractions();
            then(accountApiClient).should().refundReservedStock(USERNAME, STOCK_CODE, 10);
            then(accountApiClient).should().refundReservedLeverageStock(USERNAME, STOCK_CODE, "X2", 10);
        }

        @DisplayName("등록 후 Redis 응답 저장이 실패해도 예외 없이 응답을 발행한다 (재시도로 인한 중복 등록 방지)")
        @Test
        void givenResponseSaveFails_whenRegistering_thenDoesNotThrow() {
            given(accountApiClient.reserveCash(USERNAME, BUY_RESERVE)).willReturn(true);
            givenSaveAssignsId();
            willThrow(new IllegalStateException("redis down"))
                    .given(stockServerTrailingStopResponseRepository).save(any());

            sut.registerTrailingStop(request(TrailingStopType.BUY, LeverageRatio.SPOT));

            then(trailingStopResponseEventPublisher).should().publish(any());
            then(accountApiClient).should(never()).refundReservedCash(anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class StatusChange {

        @DisplayName("발동: ACTIVE만 TRIGGERED로 바꾼다")
        @Test
        void givenActive_whenTriggering_thenTriggered() {
            TrailingStopEntity entity = givenEntity(TrailingStopStatus.ACTIVE);

            var result = sut.updateTrailingStopStatusByTrigger(1L);

            assertThat(result.previousStatus()).isEqualTo(TrailingStopStatus.ACTIVE);
            assertThat(entity.getTrailingStopStatus()).isEqualTo(TrailingStopStatus.TRIGGERED);
        }

        @DisplayName("발동·취소: 이미 종료된 트레일링 스탑은 상태를 바꾸지 않고 이전 상태를 돌려준다")
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = TrailingStopStatus.class, names = {"CANCELED", "TRIGGERED"})
        void givenClosed_whenChanging_thenUnchanged(TrailingStopStatus status) {
            TrailingStopEntity entity = givenEntity(status);

            assertThat(sut.updateTrailingStopStatusByTrigger(1L).previousStatus()).isEqualTo(status);
            assertThat(sut.updateTrailingStopStatusByCancel(1L).previousStatus()).isEqualTo(status);
            assertThat(entity.getTrailingStopStatus()).isEqualTo(status);
        }

        @DisplayName("강제 취소: ACTIVE는 소유자 검증 없이 CANCELED로 바꾼다")
        @Test
        void givenActive_whenForceCancel_thenCanceled() {
            TrailingStopEntity entity = givenEntity(TrailingStopStatus.ACTIVE);

            assertThat(sut.updateTrailingStopStatusByCancel(1L).previousStatus()).isEqualTo(TrailingStopStatus.ACTIVE);
            assertThat(entity.getTrailingStopStatus()).isEqualTo(TrailingStopStatus.CANCELED);
        }

        @DisplayName("사용자 취소: 소유자·종목이 다르거나 요청자가 null이면 empty, 일치하면 CANCELED")
        @Test
        void givenOwnership_whenUserCancel_thenChecksOwner() {
            TrailingStopEntity entity = givenEntity(TrailingStopStatus.ACTIVE);

            assertThat(sut.updateTrailingStopStatusByUserCancel(1L, "other", STOCK_CODE)).isEmpty();
            assertThat(sut.updateTrailingStopStatusByUserCancel(1L, USERNAME, "000660")).isEmpty();
            assertThat(sut.updateTrailingStopStatusByUserCancel(1L, null, STOCK_CODE)).isEmpty();
            assertThat(entity.getTrailingStopStatus()).isEqualTo(TrailingStopStatus.ACTIVE);

            assertThat(sut.updateTrailingStopStatusByUserCancel(1L, USERNAME, STOCK_CODE)).isPresent();
            assertThat(entity.getTrailingStopStatus()).isEqualTo(TrailingStopStatus.CANCELED);
        }

        @DisplayName("트레일링 스탑이 없으면 예외를 던진다")
        @Test
        void givenNotFound_whenChanging_thenThrows() {
            given(trailingStopRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateTrailingStopStatusByTrigger(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("trailing stop not found");
            assertThatThrownBy(() -> sut.updateTrailingStopStatusByCancel(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("trailing stop not found");
            assertThatThrownBy(() -> sut.updateTrailingStopStatusByUserCancel(1L, USERNAME, STOCK_CODE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("trailing stop not found");
        }

        @DisplayName("미발동 트레일링 스탑 조회를 저장소에 위임한다")
        @Test
        void whenFindingUntriggered_thenDelegates() {
            List<TrailingStopEntity> entities = List.of(new TrailingStopEntity());
            given(trailingStopRepository.findAllUntriggered(List.of(STOCK_CODE))).willReturn(entities);

            assertThat(sut.findAllUntriggeredTrailingStops(List.of(STOCK_CODE))).isSameAs(entities);
        }
    }

    // ===== helpers =====

    private StockServerTrailingStopRequestEvent request(TrailingStopType type, LeverageRatio ratio) {
        return StockServerTrailingStopRequestEvent.of(USERNAME, STOCK_CODE, type, 10, 3.0, 70_000, ratio);
    }

    private void givenSaveAssignsId() {
        given(trailingStopRepository.save(any(TrailingStopEntity.class))).willAnswer(invocation -> {
            TrailingStopEntity entity = invocation.getArgument(0);
            entity.setTrailingStopId(1L);
            return entity;
        });
    }

    private TrailingStopEntity givenEntity(TrailingStopStatus status) {
        TrailingStopEntity entity = TrailingStopEntity.of(USERNAME, STOCK_CODE, TrailingStopType.SELL,
                LeverageRatio.SPOT, 10, 3.0, 70_000, 67_900, status);
        entity.setTrailingStopId(1L);
        given(trailingStopRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
        return entity;
    }
}

package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.StockServerOtocoRequestEvent;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] OTOCO 등록 테스트")
@ExtendWith(MockitoExtension.class)
class OtocoServiceTest {

    // 진입 70,000 × 10주 = 700,000 + 수수료 105
    private static final long SPOT_RESERVE = 700_105L;

    @InjectMocks private OtocoService sut;

    @Mock private OtocoRepository otocoRepository;
    @Mock private OtocoEntryBookRegistry otocoEntryBookRegistry;
    @Mock private OtocoResponseEventPublisher otocoResponseEventPublisher;
    @Mock private StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    @Mock private AccountApiClient accountApiClient;
    @Spy private ReserveAmountCalculator reserveAmountCalculator = new ReserveAmountCalculator();

    @DisplayName("진입가 기준 금액+수수료를 예약하고 저장·진입 북 등록·응답 저장·발행한다")
    @Test
    void givenValid_whenRegistering_thenReservesAndRegisters() {
        given(accountApiClient.reserveCash("user", SPOT_RESERVE)).willReturn(true);
        givenSaveAssignsId();

        sut.registerOtoco(request(OtocoExitMode.PCT, null, 5.0, OtocoExitMode.PCT, null, 3.0, null));

        ArgumentCaptor<OtocoEntity> captor = ArgumentCaptor.forClass(OtocoEntity.class);
        then(otocoRepository).should().save(captor.capture());
        OtocoEntity saved = captor.getValue();
        assertThat(saved.getTpTriggerPrice()).isEqualTo(73_500);
        assertThat(saved.getSlTriggerPrice()).isEqualTo(67_900);
        assertThat(saved.getLeverageRatio()).isEqualTo(LeverageRatio.SPOT);
        assertThat(saved.getOtocoStatus()).isEqualTo(OtocoStatus.WAITING_ENTRY);

        then(otocoEntryBookRegistry).should().register(argThat(d -> d.otocoId().equals(1L)));
        then(stockServerOtocoResponseRepository).should().save(any());
        then(otocoResponseEventPublisher).should().publish(any());
    }

    @DisplayName("레버리지면 증거금+수수료를 예약한다")
    @Test
    void givenLeverage_whenRegistering_thenReservesMargin() {
        given(accountApiClient.reserveCash("user", 350_105L)).willReturn(true);
        givenSaveAssignsId();

        sut.registerOtoco(request(OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 67_900, null, LeverageRatio.X2));

        then(accountApiClient).should().reserveCash("user", 350_105L);
    }

    @DisplayName("익절가가 진입가 이하이거나 손절가가 진입가 이상이면 예약 없이 에러를 발행한다")
    @Test
    void givenInvalidExitPrices_whenRegistering_thenPublishesError() {
        var badTp = request(OtocoExitMode.PRICE, 70_000, null, OtocoExitMode.PRICE, 67_900, null, null);
        var badSl = request(OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 70_000, null, null);

        sut.registerOtoco(badTp);
        sut.registerOtoco(badSl);

        then(otocoResponseEventPublisher).should().publishError(badTp, OtocoResultCode.INVALID_TP_PRICE);
        then(otocoResponseEventPublisher).should().publishError(badSl, OtocoResultCode.INVALID_SL_PRICE);
        then(accountApiClient).shouldHaveNoInteractions();
    }

    @DisplayName("예약에 실패하면 잔고 부족 에러를 발행하고 저장하지 않는다")
    @Test
    void givenReserveFails_whenRegistering_thenPublishesError() {
        var request = request(OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 67_900, null, null);
        given(accountApiClient.reserveCash("user", SPOT_RESERVE)).willReturn(false);

        sut.registerOtoco(request);

        then(otocoResponseEventPublisher).should().publishError(request, OtocoResultCode.INSUFFICIENT_BALANCE);
        then(otocoRepository).shouldHaveNoInteractions();
    }

    @DisplayName("저장 후 북 등록이 실패하면 북에서 제거·CANCELED로 무효화하고 환불·에러 발행 후 다시 던진다")
    @Test
    void givenBookFails_whenRegistering_thenCancelsAndRefunds() {
        var request = request(OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 67_900, null, null);
        given(accountApiClient.reserveCash("user", SPOT_RESERVE)).willReturn(true);
        givenSaveAssignsId();
        willThrow(new IllegalStateException("book error")).given(otocoEntryBookRegistry).register(any());

        assertThatThrownBy(() -> sut.registerOtoco(request)).hasMessage("book error");

        ArgumentCaptor<OtocoEntity> captor = ArgumentCaptor.forClass(OtocoEntity.class);
        then(otocoRepository).should(times(2)).save(captor.capture());
        assertThat(captor.getValue().getOtocoStatus()).isEqualTo(OtocoStatus.CANCELED);
        then(otocoEntryBookRegistry).should().remove("005930", 1L);
        then(accountApiClient).should().refundReservedCash("user", SPOT_RESERVE);
        then(otocoResponseEventPublisher).should().publishError(request, OtocoResultCode.INTERNAL_ERROR);
    }

    @DisplayName("무효화 저장까지 실패하면 원래 예외에 첨부하고 환불은 진행한다")
    @Test
    void givenCancelSaveFails_whenRegistering_thenSuppressed() {
        given(accountApiClient.reserveCash("user", SPOT_RESERVE)).willReturn(true);
        given(otocoRepository.save(any(OtocoEntity.class)))
                .willAnswer(invocation -> {
                    OtocoEntity entity = invocation.getArgument(0);
                    entity.setOtocoId(1L);
                    return entity;
                })
                .willThrow(new IllegalStateException("cancel save error"));
        willThrow(new IllegalStateException("book error")).given(otocoEntryBookRegistry).register(any());

        assertThatThrownBy(() -> sut.registerOtoco(
                request(OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 67_900, null, null)))
                .hasMessage("book error")
                .satisfies(e -> assertThat(e.getSuppressed()).singleElement()
                        .extracting(Throwable::getMessage).isEqualTo("cancel save error"));

        then(accountApiClient).should().refundReservedCash("user", SPOT_RESERVE);
    }

    @DisplayName("저장 자체가 실패하면 무효화 없이 환불만 하고 다시 던진다")
    @Test
    void givenSaveFails_whenRegistering_thenRefundsOnly() {
        given(accountApiClient.reserveCash("user", SPOT_RESERVE)).willReturn(true);
        given(otocoRepository.save(any())).willThrow(new IllegalStateException("db error"));

        assertThatThrownBy(() -> sut.registerOtoco(
                request(OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 67_900, null, null)))
                .hasMessage("db error");

        then(otocoEntryBookRegistry).shouldHaveNoInteractions();
        then(accountApiClient).should().refundReservedCash("user", SPOT_RESERVE);
    }

    @DisplayName("등록 후 응답 캐시 저장이 실패해도 예외 없이 발행한다 (재시도로 인한 중복 등록 방지)")
    @Test
    void givenResponseSaveFails_whenRegistering_thenDoesNotThrow() {
        given(accountApiClient.reserveCash("user", SPOT_RESERVE)).willReturn(true);
        givenSaveAssignsId();
        willThrow(new IllegalStateException("redis down")).given(stockServerOtocoResponseRepository).save(any());

        sut.registerOtoco(request(OtocoExitMode.PRICE, 73_500, null, OtocoExitMode.PRICE, 67_900, null, null));

        then(otocoResponseEventPublisher).should().publish(any());
        then(accountApiClient).should(never()).refundReservedCash(anyString(), anyLong());
    }

    @DisplayName("조회 메서드는 저장소에 위임한다")
    @Test
    void whenFinding_thenDelegates() {
        OtocoEntity entity = new OtocoEntity();
        given(otocoRepository.findByIdForUpdate(1L)).willReturn(java.util.Optional.of(entity));
        given(otocoRepository.findAllUnfinished(List.of("005930"))).willReturn(List.of(entity));

        assertThat(sut.findByIdForUpdate(1L)).containsSame(entity);
        assertThat(sut.findAllUnfinishedOtocos(List.of("005930"))).containsExactly(entity);
    }

    private StockServerOtocoRequestEvent request(OtocoExitMode tpMode, Integer tpPrice, Double tpPct,
                                                 OtocoExitMode slMode, Integer slPrice, Double slPct,
                                                 LeverageRatio ratio) {
        return new StockServerOtocoRequestEvent("user", "005930", OtocoEntryDirection.BELOW, 10, 70_000,
                tpMode, tpPrice, tpPct, slMode, slPrice, slPct, ratio);
    }

    private void givenSaveAssignsId() {
        given(otocoRepository.save(any(OtocoEntity.class))).willAnswer(invocation -> {
            OtocoEntity entity = invocation.getArgument(0);
            entity.setOtocoId(1L);
            return entity;
        });
    }
}

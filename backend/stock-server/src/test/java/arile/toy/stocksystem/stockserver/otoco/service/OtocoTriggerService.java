package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.OtocoLockRegistry;
import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoEntryDirection;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoLeg;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] OTOCO 진입·청산 발동 감시 테스트")
@ExtendWith(MockitoExtension.class)
class OtocoTriggerServiceTest {

    @Mock private OtocoLockRegistry otocoLockRegistry;

    @BeforeEach
    void setUp() {
        given(otocoLockRegistry.lock("005930")).willReturn(new ReentrantLock());
    }

    @Nested
    @DisplayName("진입")
    class Entry {

        private OtocoEntryTriggerService sut;
        @Mock private OtocoEntryBookRegistry otocoEntryBookRegistry;
        @Mock private OtocoEntryTransactionalService otocoEntryTransactionalService;

        @BeforeEach
        void inject() {
            sut = new OtocoEntryTriggerService(otocoEntryBookRegistry, otocoLockRegistry, otocoEntryTransactionalService);
        }

        @DisplayName("ABOVE는 현재가 ≥ 진입가, BELOW는 현재가 ≤ 진입가일 때 북에서 제거하고 진입한다")
        @Test
        void givenDirections_whenTick_thenTriggersMatching() {
            OtocoDto above = OtocoFixtures.dto(1L, OtocoEntryDirection.ABOVE, OtocoStatus.WAITING_ENTRY);
            OtocoDto below = OtocoFixtures.dto(2L, OtocoEntryDirection.BELOW, OtocoStatus.WAITING_ENTRY);
            given(otocoEntryBookRegistry.getAll("005930")).willReturn(List.of(above, below));

            sut.getExternalTickMessageAndTriggerEntry(tick(70_000));

            then(otocoEntryBookRegistry).should().remove("005930", 1L);
            then(otocoEntryBookRegistry).should().remove("005930", 2L);
            then(otocoEntryTransactionalService).should().triggerEntryAndRegisterOrder(above);
            then(otocoEntryTransactionalService).should().triggerEntryAndRegisterOrder(below);
        }

        @DisplayName("조건을 만족하지 않으면 발동하지 않는다 (ABOVE는 진입가 미만, BELOW는 진입가 초과)")
        @Test
        void givenNotReached_whenTick_thenNothing() {
            given(otocoEntryBookRegistry.getAll("005930"))
                    .willReturn(List.of(OtocoFixtures.dto(1L, OtocoEntryDirection.ABOVE, OtocoStatus.WAITING_ENTRY)))
                    .willReturn(List.of(OtocoFixtures.dto(2L, OtocoEntryDirection.BELOW, OtocoStatus.WAITING_ENTRY)));

            sut.getExternalTickMessageAndTriggerEntry(tick(69_900));
            sut.getExternalTickMessageAndTriggerEntry(tick(70_100));

            then(otocoEntryTransactionalService).shouldHaveNoInteractions();
            then(otocoEntryBookRegistry).should(never()).remove(any(), any());
        }

        @DisplayName("진입 처리 실패 시 북에 다시 등록하고 같은 틱의 다음 OTOCO를 계속 처리한다")
        @Test
        void givenFailure_whenTick_thenReRegistersAndContinues() {
            OtocoDto first = OtocoFixtures.dto(1L, OtocoEntryDirection.BELOW, OtocoStatus.WAITING_ENTRY);
            OtocoDto second = OtocoFixtures.dto(2L, OtocoEntryDirection.BELOW, OtocoStatus.WAITING_ENTRY);
            given(otocoEntryBookRegistry.getAll("005930")).willReturn(List.of(first, second));
            willThrow(new IllegalStateException("db error"))
                    .given(otocoEntryTransactionalService).triggerEntryAndRegisterOrder(first);

            sut.getExternalTickMessageAndTriggerEntry(tick(70_000));

            then(otocoEntryBookRegistry).should().register(first);
            then(otocoEntryTransactionalService).should().triggerEntryAndRegisterOrder(second);
        }
    }

    @Nested
    @DisplayName("청산")
    class Exit {

        private OtocoExitTriggerService sut;
        @Mock private OtocoExitBookRegistry otocoExitBookRegistry;
        @Mock private OtocoExitTransactionalService otocoExitTransactionalService;

        @BeforeEach
        void inject() {
            sut = new OtocoExitTriggerService(otocoExitBookRegistry, otocoLockRegistry, otocoExitTransactionalService);
        }

        @DisplayName("현재가 ≤ 손절가면 STOP_LOSS, ≥ 익절가면 TAKE_PROFIT으로 청산한다")
        @Test
        void givenHit_whenTick_thenExitsWithLeg() {
            OtocoDto dto = OtocoFixtures.dto(OtocoStatus.WAITING_EXIT);
            given(otocoExitBookRegistry.getAll("005930")).willReturn(List.of(dto));

            sut.getExternalTickMessageAndSettleExit(tick(67_900));
            sut.getExternalTickMessageAndSettleExit(tick(73_500));

            then(otocoExitTransactionalService).should().triggerExit(dto, OtocoLeg.STOP_LOSS);
            then(otocoExitTransactionalService).should().triggerExit(dto, OtocoLeg.TAKE_PROFIT);
            then(otocoExitBookRegistry).should(times(2)).remove("005930", 1L);
        }

        @DisplayName("손절가와 익절가 사이면 청산하지 않는다")
        @Test
        void givenBetween_whenTick_thenNothing() {
            given(otocoExitBookRegistry.getAll("005930")).willReturn(List.of(OtocoFixtures.dto(OtocoStatus.WAITING_EXIT)));

            sut.getExternalTickMessageAndSettleExit(tick(70_000));

            then(otocoExitTransactionalService).shouldHaveNoInteractions();
            then(otocoExitBookRegistry).should(never()).remove(any(), any());
        }

        @DisplayName("청산 처리 실패 시 북에 다시 등록하고 다음 OTOCO를 계속 처리한다")
        @Test
        void givenFailure_whenTick_thenReRegistersAndContinues() {
            OtocoDto first = OtocoFixtures.dto(1L, OtocoEntryDirection.BELOW, OtocoStatus.WAITING_EXIT);
            OtocoDto second = OtocoFixtures.dto(2L, OtocoEntryDirection.BELOW, OtocoStatus.WAITING_EXIT);
            given(otocoExitBookRegistry.getAll("005930")).willReturn(List.of(first, second));
            willThrow(new IllegalStateException("order error"))
                    .given(otocoExitTransactionalService).triggerExit(first, OtocoLeg.STOP_LOSS);

            sut.getExternalTickMessageAndSettleExit(tick(67_000));

            then(otocoExitBookRegistry).should().register(first);
            then(otocoExitTransactionalService).should().triggerExit(second, OtocoLeg.STOP_LOSS);
        }
    }

    private TradePriceTickMessage tick(int price) {
        TradePriceTickMessage tick = mock(TradePriceTickMessage.class);
        given(tick.stockCode()).willReturn("005930");
        given(tick.curPrice()).willReturn(price);
        return tick;
    }
}

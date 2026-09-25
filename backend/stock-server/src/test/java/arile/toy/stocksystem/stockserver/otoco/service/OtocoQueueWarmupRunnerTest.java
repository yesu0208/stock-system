package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.OtocoFixtures;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 서버 시작 시 OTOCO 북 복구 테스트")
@ExtendWith(MockitoExtension.class)
class OtocoQueueWarmupRunnerTest {

    @InjectMocks private OtocoQueueWarmupRunner sut;
    @Mock private OtocoRepository otocoRepository;
    @Mock private OtocoEntryBookRegistry otocoEntryBookRegistry;
    @Mock private OtocoExitBookRegistry otocoExitBookRegistry;
    @Mock private ExternalStockProperties externalStockProperties;

    @DisplayName("담당 종목이 없으면 조회하지 않는다")
    @Test
    void givenNoStocks_whenWarmingUp_thenNothing() {
        given(externalStockProperties.getOpen()).willReturn(List.of());

        sut.warmUp();

        then(otocoRepository).shouldHaveNoInteractions();
    }

    @DisplayName("WAITING_ENTRY는 진입 북, WAITING_EXIT는 청산 북에 복구하고 ENTRY_ORDER_PLACED는 건너뛴다 (주문 큐가 복구)")
    @Test
    void givenUnfinished_whenWarmingUp_thenRestoresByStatus() {
        given(externalStockProperties.getOpen()).willReturn(List.of("005930"));
        given(otocoRepository.findAllUnfinished(List.of("005930"))).willReturn(List.of(
                OtocoFixtures.entity(1L, OtocoStatus.WAITING_ENTRY, LeverageRatio.SPOT),
                OtocoFixtures.entity(2L, OtocoStatus.ENTRY_ORDER_PLACED, LeverageRatio.SPOT),
                OtocoFixtures.entity(3L, OtocoStatus.WAITING_EXIT, LeverageRatio.SPOT)));

        sut.warmUp();

        then(otocoEntryBookRegistry).should().register(argThat(d -> d.otocoId().equals(1L)));
        then(otocoExitBookRegistry).should().register(argThat(d -> d.otocoId().equals(3L)));
        then(otocoEntryBookRegistry).shouldHaveNoMoreInteractions();
        then(otocoExitBookRegistry).shouldHaveNoMoreInteractions();
    }
}

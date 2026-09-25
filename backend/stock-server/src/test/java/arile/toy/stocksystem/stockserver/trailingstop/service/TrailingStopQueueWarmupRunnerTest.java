package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.registry.TrailingStopBookRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 서버 시작 시 트레일링 스탑 북 복구 테스트")
@ExtendWith(MockitoExtension.class)
class TrailingStopQueueWarmupRunnerTest {

    @InjectMocks private TrailingStopQueueWarmupRunner sut;

    @Mock private TrailingStopService trailingStopService;
    @Mock private TrailingStopBookRegistry trailingStopBookRegistry;
    @Mock private ExternalStockProperties externalStockProperties;

    @DisplayName("담당 종목이 없으면 조회·복구를 하지 않는다")
    @Test
    void givenNoStocks_whenWarmingUp_thenDoesNothing() {
        given(externalStockProperties.getOpen()).willReturn(List.of());

        sut.warmUp();

        then(trailingStopService).shouldHaveNoInteractions();
        then(trailingStopBookRegistry).shouldHaveNoInteractions();
    }

    @DisplayName("저장된 추적 상태에서 추적을 이어가도록 북에 복구하고, 초기 발동가는 등록 시점 값을 유지한다")
    @Test
    void givenPersistedTrail_whenWarmingUp_thenResumesTrail() {
        TrailingStopEntity trailed = entity(1L);
        trailed.setCurrentBasePrice(72_000);
        trailed.setCurrentTriggerPrice(69_800);
        TrailingStopEntity notTrailed = entity(2L);
        given(externalStockProperties.getOpen()).willReturn(List.of("005930"));
        given(trailingStopService.findAllUntriggeredTrailingStops(List.of("005930")))
                .willReturn(List.of(trailed, notTrailed));

        sut.warmUp();

        ArgumentCaptor<TrailingStopDto> captor = ArgumentCaptor.forClass(TrailingStopDto.class);
        then(trailingStopBookRegistry).should(times(2)).register(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(TrailingStopDto::trailingStopId, TrailingStopDto::basePrice,
                        TrailingStopDto::triggerPrice, TrailingStopDto::initialTriggerPrice)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(1L, 72_000, 69_800, 67_900),
                        org.assertj.core.api.Assertions.tuple(2L, 70_000, 67_900, 67_900));
    }

    @DisplayName("미발동 트레일링 스탑이 없으면 아무것도 등록하지 않는다")
    @Test
    void givenNone_whenWarmingUp_thenRegistersNothing() {
        given(externalStockProperties.getOpen()).willReturn(List.of("005930"));
        given(trailingStopService.findAllUntriggeredTrailingStops(List.of("005930"))).willReturn(List.of());

        sut.warmUp();

        then(trailingStopBookRegistry).should(never()).register(any());
    }

    private TrailingStopEntity entity(Long id) {
        TrailingStopEntity entity = TrailingStopEntity.of("user", "005930", TrailingStopType.SELL,
                LeverageRatio.SPOT, 10, 3.0, 70_000, 67_900, TrailingStopStatus.ACTIVE);
        entity.setTrailingStopId(id);
        return entity;
    }
}

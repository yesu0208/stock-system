package arile.toy.stocksystem.stockserver.autoorder.service;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
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

@DisplayName("[Service] 서버 시작 시 미발동 자동주문 대기열 복구 테스트")
@ExtendWith(MockitoExtension.class)
class AutoOrderQueueWarmupRunnerTest {

    @InjectMocks private AutoOrderQueueWarmupRunner sut;

    @Mock private AutoOrderService autoOrderService;
    @Mock private AutoOrderQueueRegistry autoOrderQueueRegistry;
    @Mock private ExternalStockProperties externalStockProperties;

    @DisplayName("담당 종목이 없으면 조회·복구를 하지 않는다")
    @Test
    void givenNoStocks_whenWarmingUp_thenDoesNothing() {
        given(externalStockProperties.getOpen()).willReturn(List.of());

        sut.warmUp();

        then(autoOrderService).shouldHaveNoInteractions();
        then(autoOrderQueueRegistry).shouldHaveNoInteractions();
    }

    @DisplayName("담당 종목의 미발동 자동주문을 저장된 발동가·등록 시각 그대로 대기열에 복구한다")
    @Test
    void givenUntriggered_whenWarmingUp_thenEnqueuesEach() {
        List<String> stockCodes = List.of("005930");
        given(externalStockProperties.getOpen()).willReturn(stockCodes);
        AutoOrderEntity buy = entity(1L, AutoOrderType.BUY, 71_000);
        AutoOrderEntity sell = entity(2L, AutoOrderType.SELL, 69_000);
        given(autoOrderService.findAllUntriggeredAutoOrders(stockCodes)).willReturn(List.of(buy, sell));

        sut.warmUp();

        ArgumentCaptor<AutoOrderDto> captor = ArgumentCaptor.forClass(AutoOrderDto.class);
        then(autoOrderQueueRegistry).should(times(2)).autoOrderEnqueue(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AutoOrderDto::autoOrderId, AutoOrderDto::triggerPrice, AutoOrderDto::orderTime)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(1L, 71_000, buy.getOrderTime()),
                        org.assertj.core.api.Assertions.tuple(2L, 69_000, sell.getOrderTime()));
    }

    @DisplayName("미발동 자동주문이 없으면 대기열에 아무것도 등록하지 않는다")
    @Test
    void givenNoUntriggered_whenWarmingUp_thenEnqueuesNothing() {
        given(externalStockProperties.getOpen()).willReturn(List.of("005930"));
        given(autoOrderService.findAllUntriggeredAutoOrders(List.of("005930"))).willReturn(List.of());

        sut.warmUp();

        then(autoOrderQueueRegistry).should(never()).autoOrderEnqueue(any());
    }

    private AutoOrderEntity entity(Long id, AutoOrderType type, int triggerPrice) {
        AutoOrderEntity entity = AutoOrderEntity.of("user", "005930", type, LeverageRatio.SPOT,
                triggerPrice, 70_000, 10, AutoOrderStatus.ACTIVE);
        entity.setAutoOrderId(id);
        return entity;
    }
}

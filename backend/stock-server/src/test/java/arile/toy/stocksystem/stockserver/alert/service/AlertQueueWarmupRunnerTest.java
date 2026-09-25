package arile.toy.stocksystem.stockserver.alert.service;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDirection;
import arile.toy.stocksystem.stockserver.alert.dto.AlertQueueRegistry;
import arile.toy.stocksystem.stockserver.alert.dto.AlertStatus;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 서버 시작 시 알림 큐 복구 테스트")
@ExtendWith(MockitoExtension.class)
class AlertQueueWarmupRunnerTest {

    @InjectMocks private AlertQueueWarmupRunner sut;
    @Mock private AlertService alertService;
    @Mock private AlertQueueRegistry alertQueueRegistry;
    @Mock private ExternalStockProperties externalStockProperties;

    @DisplayName("담당 종목이 없으면 조회하지 않는다")
    @Test
    void givenNoStocks_whenWarmingUp_thenNothing() {
        given(externalStockProperties.getOpen()).willReturn(List.of());

        sut.warmUp();

        then(alertService).shouldHaveNoInteractions();
    }

    @DisplayName("활성 알림을 모두 큐에 복구한다")
    @Test
    void givenActiveAlerts_whenWarmingUp_thenEnqueuesAll() {
        AlertEntity first = AlertEntity.of("user", "005930", AlertDirection.ABOVE, 72_000, AlertStatus.ACTIVE);
        first.setAlertId(1L);
        AlertEntity second = AlertEntity.of("user", "005930", AlertDirection.BELOW, 68_000, AlertStatus.ACTIVE);
        second.setAlertId(2L);
        given(externalStockProperties.getOpen()).willReturn(List.of("005930"));
        given(alertService.findAllActiveAlerts(List.of("005930"))).willReturn(List.of(first, second));

        sut.warmUp();

        then(alertQueueRegistry).should().alertEnqueue(argThat(d -> d.alertId().equals(1L)));
        then(alertQueueRegistry).should().alertEnqueue(argThat(d -> d.alertId().equals(2L)));
    }
}

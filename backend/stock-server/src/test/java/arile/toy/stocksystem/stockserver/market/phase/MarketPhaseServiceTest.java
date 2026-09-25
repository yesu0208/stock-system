package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.external.stock.checker.MarketTimeChecker;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 장 상태 전환 테스트")
@ExtendWith(MockitoExtension.class)
class MarketPhaseServiceTest {

    private static final String OPEN_STOCK = "005930";
    private static final String CLOSE_GROUP_STOCK = "999999";

    @InjectMocks private MarketPhaseService sut;

    @Spy private StockServerMarketPhaseRegistry registry = new StockServerMarketPhaseRegistry();
    @Mock private ExternalStockProperties stockProperties;
    @Mock private MarketPhasePublisher marketPhasePublisher;
    @Mock private MarketTimeChecker marketTimeChecker;

    @Nested
    @DisplayName("updateMarketPhase")
    class UpdateMarketPhase {

        @DisplayName("상태가 바뀌면 레지스트리에 반영하고 발행한다")
        @Test
        void givenNewPhase_whenUpdating_thenSetsAndPublishes() {
            sut.updateMarketPhase(OPEN_STOCK, StockServerMarketPhase.OPEN);

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.OPEN);
            then(marketPhasePublisher).should().publish(OPEN_STOCK, StockServerMarketPhase.OPEN);
        }

        @DisplayName("이미 같은 상태면 다시 발행하지 않는다")
        @Test
        void givenSamePhase_whenUpdating_thenDoesNotPublish() {
            registry.setPhase(OPEN_STOCK, StockServerMarketPhase.OPEN);

            sut.updateMarketPhase(OPEN_STOCK, StockServerMarketPhase.OPEN);

            then(marketPhasePublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("closeMarketAfterClosingCall: 마감 동시호가 체결 틱으로 장 마감")
    class CloseAfterClosingCall {

        @DisplayName("15:29:50 이상 15:37:50 미만 체결 틱이면 해당 종목을 마감한다")
        @ParameterizedTest(name = "체결 시각 {0}")
        @ValueSource(strings = {"152950", "153000", "153749"})
        void givenTickInWindow_whenChecking_thenCloses(String tradeTime) {
            registry.setPhase(OPEN_STOCK, StockServerMarketPhase.CLOSING_CALL);

            sut.closeMarketAfterClosingCall(OPEN_STOCK, tradeTime);

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.CLOSED);
            then(marketPhasePublisher).should().publish(OPEN_STOCK, StockServerMarketPhase.CLOSED);
        }

        @DisplayName("구간 밖 체결 틱이면 상태를 바꾸지 않는다")
        @ParameterizedTest(name = "체결 시각 {0}")
        @ValueSource(strings = {"152949", "153750", "090000", "160010"})
        void givenTickOutsideWindow_whenChecking_thenKeepsPhase(String tradeTime) {
            registry.setPhase(OPEN_STOCK, StockServerMarketPhase.CLOSING_CALL);

            sut.closeMarketAfterClosingCall(OPEN_STOCK, tradeTime);

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.CLOSING_CALL);
            then(marketPhasePublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("setScheduledMarkets: 서버 시작 시 현재 시각 기준 상태 설정")
    class SetScheduledMarkets {

        @DisplayName("현재가 주문 가능 구간이면 담당 종목을 그 상태로, 마감 그룹 종목은 마감으로 설정한다")
        @Test
        void givenOrderablePhase_whenSetting_thenOpensAssignedStocks() {
            given(marketTimeChecker.resolvePhase()).willReturn(StockServerMarketPhase.OPEN);
            given(stockProperties.getOpen()).willReturn(List.of(OPEN_STOCK));
            given(stockProperties.getClose()).willReturn(List.of(CLOSE_GROUP_STOCK));

            sut.setScheduledMarkets();

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.OPEN);
            assertThat(registry.getPhase(CLOSE_GROUP_STOCK)).isEqualTo(StockServerMarketPhase.CLOSED);
        }

        @DisplayName("현재가 마감 구간이면 담당 종목과 마감 그룹 종목을 모두 마감으로 설정한다")
        @Test
        void givenClosedPhase_whenSetting_thenClosesAll() {
            given(marketTimeChecker.resolvePhase()).willReturn(StockServerMarketPhase.CLOSED);
            given(stockProperties.getOpen()).willReturn(List.of(OPEN_STOCK));
            given(stockProperties.getClose()).willReturn(List.of(CLOSE_GROUP_STOCK));

            sut.setScheduledMarkets();

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.CLOSED);
            assertThat(registry.getPhase(CLOSE_GROUP_STOCK)).isEqualTo(StockServerMarketPhase.CLOSED);
        }
    }

    @Nested
    @DisplayName("스케줄 전환")
    class ScheduledTransitions {

        @DisplayName("closeAllMarkets: 담당 종목과 마감 그룹 종목을 모두 마감한다")
        @Test
        void closeAllMarkets_closesBothGroups() {
            given(stockProperties.getOpen()).willReturn(List.of(OPEN_STOCK));
            given(stockProperties.getClose()).willReturn(List.of(CLOSE_GROUP_STOCK));

            sut.closeAllMarkets();

            assertThat(registry.isClosed(OPEN_STOCK)).isTrue();
            assertThat(registry.isClosed(CLOSE_GROUP_STOCK)).isTrue();
        }

        @DisplayName("openMorningCall: 담당 종목을 아침 동시호가로 전환한다")
        @Test
        void openMorningCall() {
            given(stockProperties.getOpen()).willReturn(List.of(OPEN_STOCK));

            sut.openMorningCall();

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.MORNING_CALL);
        }

        @DisplayName("openRegularMarket: 담당 종목을 정규장으로 전환한다")
        @Test
        void openRegularMarket() {
            given(stockProperties.getOpen()).willReturn(List.of(OPEN_STOCK));

            sut.openRegularMarket();

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.OPEN);
        }

        @DisplayName("openClosingCall: 담당 종목을 마감 동시호가로 전환한다")
        @Test
        void openClosingCall() {
            given(stockProperties.getOpen()).willReturn(List.of(OPEN_STOCK));

            sut.openClosingCall();

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.CLOSING_CALL);
        }

        @DisplayName("openAfterMarket: 담당 종목을 애프터마켓으로 전환한다")
        @Test
        void openAfterMarket() {
            given(stockProperties.getOpen()).willReturn(List.of(OPEN_STOCK));

            sut.openAfterMarket();

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.AFTER);
        }

        @DisplayName("closeScheduledOpenMarkets: 담당 종목만 마감한다")
        @Test
        void closeScheduledOpenMarkets() {
            given(stockProperties.getOpen()).willReturn(List.of(OPEN_STOCK));

            sut.closeScheduledOpenMarkets();

            assertThat(registry.getPhase(OPEN_STOCK)).isEqualTo(StockServerMarketPhase.CLOSED);
            then(stockProperties).should(never()).getClose();
        }
    }
}

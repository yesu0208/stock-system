package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginCallBatchResult;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.event.MarginCallEvent;
import arile.toy.stocksystem.accountserver.leverage.event.publisher.MarginCallEventPublisher;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.stockprice.dto.StockSummaryTickMessage;
import arile.toy.stocksystem.accountserver.stockprice.repository.StockSummaryRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeverageMarginCallServiceTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";
    private static final long LOAN = 350_000L;

    @Mock private LeveragePositionRepository leveragePositionRepository;
    @Mock private StockSummaryRedisRepository stockSummaryRedisRepository;
    @Mock private LeveragePositionRedisSyncer redisSyncer;
    @Mock private AccountMarginStatusSyncer accountMarginStatusSyncer;
    @Mock private MarginCallEventPublisher marginCallEventPublisher;

    private LeverageMarginCallService service;

    @BeforeEach
    void setUp() {
        service = new LeverageMarginCallService(
                leveragePositionRepository, stockSummaryRedisRepository, new MarginRatioCalculator(),
                redisSyncer, accountMarginStatusSyncer, marginCallEventPublisher);
    }

    /** X2, 10주, 매입 700,000 → 대출 350,000 */
    private static LeveragePositionEntity position(String stockCode, MarginStatus status, LocalDate marginCallDate) {
        LeveragePositionEntity position =
                LeveragePositionEntity.of(USERNAME, stockCode, LeverageRatio.X2, 10, 700_000L, 700_100L);
        position.changeMarginStatus(status, marginCallDate);
        return position;
    }

    private void givenPrice(String stockCode, Integer price) {
        given(stockSummaryRedisRepository.findByStockCode(stockCode))
                .willReturn(new StockSummaryTickMessage(stockCode, price, 0));
    }

    private static double ratioAt(int price) {
        return 10L * price * 100.0 / LOAN;
    }

    private void verifyTransitioned(LeveragePositionEntity position) {
        verify(leveragePositionRepository).save(position);
        verify(redisSyncer).sync(position);
        verify(accountMarginStatusSyncer).resync(USERNAME);
    }

    private void verifyEvent(MarginStatus status, double ratio) {
        verify(marginCallEventPublisher).publish(
                MarginCallEvent.of(USERNAME, STOCK_CODE, LeverageRatio.X2, status, ratio));
    }

    // ===================== 평가 제외 =====================

    @Nested
    @DisplayName("평가 제외 대상")
    class Skipped {

        @Test
        @DisplayName("이미 LIQUIDATION_PENDING인 포지션은 평가하지 않는다")
        void liquidationPending_skipped() {
            LeveragePositionEntity position =
                    position(STOCK_CODE, MarginStatus.LIQUIDATION_PENDING, LocalDate.now().minusDays(1));

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(0, 0, 0));
            verifyNoInteractions(stockSummaryRedisRepository, leveragePositionRepository, marginCallEventPublisher);
        }

        @Test
        @DisplayName("대출금이 없는 NORMAL 포지션은 아무것도 하지 않는다")
        void noLoanNormal_skipped() {
            LeveragePositionEntity position = position(STOCK_CODE, MarginStatus.NORMAL, null);
            position.setLoanAmount(0L);

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(0, 0, 0));
            verifyNoInteractions(stockSummaryRedisRepository, leveragePositionRepository);
        }

        @Test
        @DisplayName("주가 요약이 없으면 평가를 건너뛴다")
        void summaryNotFound_skipped() {
            LeveragePositionEntity position = position(STOCK_CODE, MarginStatus.NORMAL, null);
            given(stockSummaryRedisRepository.findByStockCode(STOCK_CODE)).willReturn(null);

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(0, 0, 0));
            verifyNoInteractions(leveragePositionRepository, marginCallEventPublisher);
        }

        @Test
        @DisplayName("현재가가 null이면 평가를 건너뛴다")
        void curPriceNull_skipped() {
            LeveragePositionEntity position = position(STOCK_CODE, MarginStatus.NORMAL, null);
            givenPrice(STOCK_CODE, null);

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(0, 0, 0));
            verifyNoInteractions(leveragePositionRepository, marginCallEventPublisher);
        }
    }

    // ===================== NORMAL 포지션 =====================

    @Nested
    @DisplayName("NORMAL 포지션")
    class FromNormal {

        @ParameterizedTest(name = "주가 {0}")
        @ValueSource(ints = {50_000, 49_000})
        @DisplayName("담보비율이 140% 이상이면 상태를 유지한다 (경계 포함)")
        void aboveMaintenance_staysNormal(int price) {
            LeveragePositionEntity position = position(STOCK_CODE, MarginStatus.NORMAL, null);
            givenPrice(STOCK_CODE, price);

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(0, 0, 0));
            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.NORMAL);
            verifyNoInteractions(leveragePositionRepository, marginCallEventPublisher);
        }

        @Test
        @DisplayName("담보비율이 140% 미만이면 오늘 날짜로 MARGIN_CALL 전환하고 이벤트를 발행한다")
        void belowMaintenance_transitionsToMarginCall() {
            LeveragePositionEntity position = position(STOCK_CODE, MarginStatus.NORMAL, null);
            givenPrice(STOCK_CODE, 48_000);

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(1, 0, 0));
            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.MARGIN_CALL);
            assertThat(position.getMarginCallDate()).isEqualTo(LocalDate.now());
            verifyTransitioned(position);
            verifyEvent(MarginStatus.MARGIN_CALL, ratioAt(48_000));
        }
    }

    // ===================== MARGIN_CALL 포지션 (재평가) =====================

    @Nested
    @DisplayName("MARGIN_CALL 포지션 재평가")
    class FromMarginCall {

        private final LocalDate marginCallDate = LocalDate.now().minusDays(1);

        @Test
        @DisplayName("담보비율이 회복되면 NORMAL로 복귀하고 마진콜 일자를 지운 뒤 회복 이벤트를 발행한다")
        void recovered_transitionsToNormal() {
            LeveragePositionEntity position = position(STOCK_CODE, MarginStatus.MARGIN_CALL, marginCallDate);
            givenPrice(STOCK_CODE, 50_000);

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(0, 1, 0));
            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.NORMAL);
            assertThat(position.getMarginCallDate()).isNull();
            verifyTransitioned(position);
            verifyEvent(MarginStatus.NORMAL, ratioAt(50_000));
        }

        @Test
        @DisplayName("여전히 미달이면 마진콜 일자를 유지한 채 LIQUIDATION_PENDING으로 전환하고 이벤트를 발행한다")
        void stillBelow_transitionsToLiquidationPending() {
            LeveragePositionEntity position = position(STOCK_CODE, MarginStatus.MARGIN_CALL, marginCallDate);
            givenPrice(STOCK_CODE, 48_000);

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(0, 0, 1));
            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.LIQUIDATION_PENDING);
            assertThat(position.getMarginCallDate()).isEqualTo(marginCallDate);
            verifyTransitioned(position);
            verifyEvent(MarginStatus.LIQUIDATION_PENDING, ratioAt(48_000));
        }

        @Test
        @DisplayName("대출금을 모두 상환했으면 주가와 무관하게 NORMAL로 복귀한다 (이벤트 미발행)")
        void noLoan_transitionsToNormalWithoutEvent() {
            LeveragePositionEntity position = position(STOCK_CODE, MarginStatus.MARGIN_CALL, marginCallDate);
            position.setLoanAmount(0L);

            MarginCallBatchResult result = service.evaluatePositions(List.of(position));

            assertThat(result).isEqualTo(new MarginCallBatchResult(0, 1, 0));
            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.NORMAL);
            verifyTransitioned(position);
            verifyNoInteractions(stockSummaryRedisRepository, marginCallEventPublisher);
        }
    }

    // ===================== 배치 =====================

    @Test
    @DisplayName("한 포지션 처리 중 예외가 나도 나머지 포지션은 계속 평가하고 집계에서 제외한다")
    void whenOnePositionFails_continuesOthers() {
        LeveragePositionEntity failing = position("000660", MarginStatus.NORMAL, null);
        LeveragePositionEntity normal = position(STOCK_CODE, MarginStatus.NORMAL, null);
        givenPrice("000660", 48_000);
        givenPrice(STOCK_CODE, 48_000);
        willThrow(new RuntimeException("db down")).given(leveragePositionRepository).save(failing);

        MarginCallBatchResult result = service.evaluatePositions(List.of(failing, normal));

        assertThat(result).isEqualTo(new MarginCallBatchResult(1, 0, 0));
        verifyTransitioned(normal);
        verifyEvent(MarginStatus.MARGIN_CALL, ratioAt(48_000));
    }

    @Test
    @DisplayName("여러 포지션의 결과를 상태별로 집계한다")
    void aggregatesResults() {
        LeveragePositionEntity newCall = position("000001", MarginStatus.NORMAL, null);
        LeveragePositionEntity recovered = position("000002", MarginStatus.MARGIN_CALL, LocalDate.now().minusDays(1));
        LeveragePositionEntity liquidation = position("000003", MarginStatus.MARGIN_CALL, LocalDate.now().minusDays(1));
        givenPrice("000001", 48_000);
        givenPrice("000002", 50_000);
        givenPrice("000003", 48_000);

        MarginCallBatchResult result = service.evaluatePositions(List.of(newCall, recovered, liquidation));

        assertThat(result).isEqualTo(new MarginCallBatchResult(1, 1, 1));
    }
}

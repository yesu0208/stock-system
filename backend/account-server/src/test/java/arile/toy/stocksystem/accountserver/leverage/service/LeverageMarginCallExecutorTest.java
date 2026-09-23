package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.event.MarginCallEvent;
import arile.toy.stocksystem.accountserver.leverage.event.publisher.MarginCallEventPublisher;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.leverage.service.LeverageMarginCallExecutor.MarginCallOutcome;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeverageMarginCallExecutorTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";
    private static final long LOAN = 350_000L;
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final LocalDate MARGIN_CALL_DATE = TODAY.minusDays(1);

    @Mock private LeveragePositionRepository leveragePositionRepository;
    @Mock private StockSummaryRedisRepository stockSummaryRedisRepository;
    @Mock private LeveragePositionRedisSyncer redisSyncer;
    @Mock private AccountMarginStatusSyncer accountMarginStatusSyncer;
    @Mock private MarginCallEventPublisher marginCallEventPublisher;

    private LeverageMarginCallExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new LeverageMarginCallExecutor(
                leveragePositionRepository, stockSummaryRedisRepository, new MarginRatioCalculator(),
                redisSyncer, accountMarginStatusSyncer, marginCallEventPublisher);
    }

    /** X2, 10주, 매입 700,000 → 대출 350,000 / 담보비율 140% 경계 주가 = 49,000 */
    private LeveragePositionEntity givenPosition(MarginStatus status, LocalDate marginCallDate) {
        LeveragePositionEntity position =
                LeveragePositionEntity.of(USERNAME, STOCK_CODE, LeverageRatio.X2, 10, 700_000L, 700_100L);
        position.changeMarginStatus(status, marginCallDate);
        given(leveragePositionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(position));
        return position;
    }

    private void givenPrice(Integer price) {
        given(stockSummaryRedisRepository.findByStockCode(STOCK_CODE))
                .willReturn(new StockSummaryTickMessage(STOCK_CODE, price, 0));
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

    private void verifyNotTransitioned() {
        verify(leveragePositionRepository, never()).save(any());
        verifyNoInteractions(redisSyncer, accountMarginStatusSyncer, marginCallEventPublisher);
    }

    // ===================== 평가 제외 =====================

    @Nested
    @DisplayName("평가 제외 대상")
    class Skipped {

        @Test
        @DisplayName("포지션이 없으면 예외를 던진다")
        void whenPositionNotFound_throws() {
            given(leveragePositionRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> executor.evaluateOnePosition(1L, TODAY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Leverage position not found. id=1");
        }

        @Test
        @DisplayName("이미 LIQUIDATION_PENDING인 포지션은 평가하지 않는다")
        void liquidationPending_skipped() {
            givenPosition(MarginStatus.LIQUIDATION_PENDING, MARGIN_CALL_DATE);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.UNCHANGED);

            verifyNoInteractions(stockSummaryRedisRepository);
            verifyNotTransitioned();
        }

        @Test
        @DisplayName("대출금이 없는 NORMAL 포지션은 아무것도 하지 않는다")
        void noLoanNormal_skipped() {
            LeveragePositionEntity position = givenPosition(MarginStatus.NORMAL, null);
            position.setLoanAmount(0L);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.UNCHANGED);

            verifyNoInteractions(stockSummaryRedisRepository);
            verifyNotTransitioned();
        }

        @Test
        @DisplayName("주가 요약이 없으면 평가를 건너뛴다")
        void summaryNotFound_skipped() {
            givenPosition(MarginStatus.NORMAL, null);
            given(stockSummaryRedisRepository.findByStockCode(STOCK_CODE)).willReturn(null);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.UNCHANGED);

            verifyNotTransitioned();
        }

        @Test
        @DisplayName("현재가가 null이면 평가를 건너뛴다")
        void curPriceNull_skipped() {
            givenPosition(MarginStatus.NORMAL, null);
            givenPrice(null);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.UNCHANGED);

            verifyNotTransitioned();
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
            LeveragePositionEntity position = givenPosition(MarginStatus.NORMAL, null);
            givenPrice(price);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.UNCHANGED);

            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.NORMAL);
            verifyNotTransitioned();
        }

        @Test
        @DisplayName("담보비율이 140% 미만이면 오늘 날짜로 MARGIN_CALL 전환하고 이벤트를 발행한다")
        void belowMaintenance_transitionsToMarginCall() {
            LeveragePositionEntity position = givenPosition(MarginStatus.NORMAL, null);
            givenPrice(48_000);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.NEW_MARGIN_CALL);

            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.MARGIN_CALL);
            assertThat(position.getMarginCallDate()).isEqualTo(TODAY);
            verifyTransitioned(position);
            verifyEvent(MarginStatus.MARGIN_CALL, ratioAt(48_000));
        }
    }

    // ===================== MARGIN_CALL 포지션 (재평가) =====================

    @Nested
    @DisplayName("MARGIN_CALL 포지션 재평가")
    class FromMarginCall {

        @Test
        @DisplayName("담보비율이 회복되면 NORMAL로 복귀하고 마진콜 일자를 지운 뒤 회복 이벤트를 발행한다")
        void recovered_transitionsToNormal() {
            LeveragePositionEntity position = givenPosition(MarginStatus.MARGIN_CALL, MARGIN_CALL_DATE);
            givenPrice(50_000);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.RECOVERED);

            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.NORMAL);
            assertThat(position.getMarginCallDate()).isNull();
            verifyTransitioned(position);
            verifyEvent(MarginStatus.NORMAL, ratioAt(50_000));
        }

        @Test
        @DisplayName("여전히 미달이면 마진콜 일자를 유지한 채 LIQUIDATION_PENDING으로 전환하고 이벤트를 발행한다")
        void stillBelow_transitionsToLiquidationPending() {
            LeveragePositionEntity position = givenPosition(MarginStatus.MARGIN_CALL, MARGIN_CALL_DATE);
            givenPrice(48_000);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.QUEUED_FOR_LIQUIDATION);

            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.LIQUIDATION_PENDING);
            assertThat(position.getMarginCallDate()).isEqualTo(MARGIN_CALL_DATE);
            verifyTransitioned(position);
            verifyEvent(MarginStatus.LIQUIDATION_PENDING, ratioAt(48_000));
        }

        @Test
        @DisplayName("대출금을 모두 상환했으면 주가와 무관하게 NORMAL로 복귀한다 (이벤트 미발행)")
        void noLoan_transitionsToNormalWithoutEvent() {
            LeveragePositionEntity position = givenPosition(MarginStatus.MARGIN_CALL, MARGIN_CALL_DATE);
            position.setLoanAmount(0L);

            assertThat(executor.evaluateOnePosition(1L, TODAY)).isEqualTo(MarginCallOutcome.RECOVERED);

            assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.NORMAL);
            verifyTransitioned(position);
            verifyNoInteractions(stockSummaryRedisRepository, marginCallEventPublisher);
        }
    }
}

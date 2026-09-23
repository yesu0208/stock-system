package arile.toy.stocksystem.accountserver.rank.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RankScoreCalculatorTest {

    private static final long PREV = 1_000_000L; // 1만 원 = 1%

    private final RankScoreCalculator calculator = new RankScoreCalculator();

    @ParameterizedTest(name = "총자산 {0} → 수익 점수 {1}")
    @CsvSource({
            "1000000, 0", // 0%
            "1000100, 1", // +0.01%: 0.01%당 1점
            "1050000, 500", // +5%
            "1100000, 1000", // +10% (1구간 경계)
            "1150000, 1350", // +15%: 1,000 + 5% × 70
            "1200000, 1700", // +20% (2구간 경계)
            "1250000, 1950" // +25%: 1,700 + 5% × 50
    })
    @DisplayName("수익: 0~10%는 1%당 100점, 10~20%는 초과분 1%당 70점, 20% 초과는 초과분 1%당 50점")
    void positiveScore(long todayAsset, double expected) {
        assertThat(calculator.calculateDailyDelta(PREV, todayAsset, 0L)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "총자산 {0} → 손실 점수 {1}")
    @CsvSource({
            "999900, -1", // -0.01%: 0.9점 → 반올림 1점
            "950000, -450", // -5%
            "900000, -900", // -10% (1구간 경계)
            "850000, -1250", // -15%: 900 + 5% × 70
            "800000, -1600", // -20% (2구간 경계)
            "750000, -1850" // -25%: 1,600 + 5% × 50
    })
    @DisplayName("손실: 0~10%는 1%당 90점, 10~20%는 초과분 1%당 70점, 20% 초과는 초과분 1%당 50점 차감")
    void negativeScore(long todayAsset, double expected) {
        assertThat(calculator.calculateDailyDelta(PREV, todayAsset, 0L)).isEqualTo(expected);
    }

    @Test
    @DisplayName("전일 총자산이 0이면 수익률을 0으로 보고 거래대금 점수만 반영한다")
    void whenPreviousAssetZero_onlyTradePoint() {
        assertThat(calculator.calculateDailyDelta(0L, 1_000_000L, 20_000_000L)).isEqualTo(2.0);
    }

    @ParameterizedTest(name = "거래대금 {0} → {1}점")
    @CsvSource({
            "0, 0",
            "9999999, 0", // 1천만 원 미만은 0점
            "10000000, 1", // 1천만 원당 1점
            "155000000, 15", // 내림
            "300000000, 30", // 최대 인정 금액
            "1000000000, 30" // 3억 원 초과분은 인정하지 않음
    })
    @DisplayName("거래대금 점수: 1천만 원당 1점, 최대 30점")
    void tradePoint(long dailyTradeAmount, double expected) {
        assertThat(calculator.calculateDailyDelta(PREV, PREV, dailyTradeAmount)).isEqualTo(expected);
    }

    @Test
    @DisplayName("수익 점수와 거래대금 점수를 합산한다")
    void sumsProfitAndTradePoint() {
        // +5%(500점) + 거래대금 5천만 원(5점)
        assertThat(calculator.calculateDailyDelta(PREV, 1_050_000L, 50_000_000L)).isEqualTo(505.0);
    }

    @Test
    @DisplayName("손실이어도 거래대금 점수는 더해진다")
    void lossWithTradePoint() {
        // -5%(-450점) + 거래대금 1억 원(10점)
        assertThat(calculator.calculateDailyDelta(PREV, 950_000L, 100_000_000L)).isEqualTo(-440.0);
    }
}

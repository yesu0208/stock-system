package arile.toy.stocksystem.accountserver.leverage.entity;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LeveragePositionEntityTest {

    private static LeveragePositionEntity position(int quantity, long purchaseAmount, long costAmount) {
        return LeveragePositionEntity.of("user1", "005930", LeverageRatio.X2, quantity, purchaseAmount, costAmount);
    }

    @Test
    @DisplayName("of: 매도 가능 수량은 보유 수량과 같고, 대출금은 배율로 계산하며, NORMAL·오늘 청구일로 시작한다")
    void of() {
        LeveragePositionEntity position = position(10, 700_000L, 700_100L);

        assertThat(position.getUsername()).isEqualTo("user1");
        assertThat(position.getStockCode()).isEqualTo("005930");
        assertThat(position.getLeverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(position.getQuantity()).isEqualTo(10);
        assertThat(position.getAvailableQuantity()).isEqualTo(10);
        assertThat(position.getPurchaseAmount()).isEqualTo(700_000L);
        assertThat(position.getCostAmount()).isEqualTo(700_100L);
        assertThat(position.getLoanAmount()).isEqualTo(350_000L);
        assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.NORMAL);
        assertThat(position.getMarginCallDate()).isNull();
        assertThat(position.getLastInterestChargedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("addPurchase: 수량·매도 가능 수량·매입금액·원가·대출금을 모두 누적한다")
    void addPurchase() {
        LeveragePositionEntity position = position(10, 700_000L, 700_100L);
        position.setAvailableQuantity(6);

        position.addPurchase(5, 345_000L, 345_050L, 172_500L);

        assertThat(position.getQuantity()).isEqualTo(15);
        assertThat(position.getAvailableQuantity()).isEqualTo(11);
        assertThat(position.getPurchaseAmount()).isEqualTo(1_045_000L);
        assertThat(position.getCostAmount()).isEqualTo(1_045_150L);
        assertThat(position.getLoanAmount()).isEqualTo(522_500L);
    }

    @Test
    @DisplayName("reduceBySell: 매도 수량 비율만큼 매입금액·원가·대출금을 차감하고 상환 대출금을 반환한다")
    void reduceBySell() {
        LeveragePositionEntity position = position(10, 700_000L, 700_100L); // 대출 350,000

        long repaid = position.reduceBySell(4);

        assertThat(repaid).isEqualTo(140_000L);
        assertThat(position.getQuantity()).isEqualTo(6);
        assertThat(position.getPurchaseAmount()).isEqualTo(420_000L);
        assertThat(position.getCostAmount()).isEqualTo(420_060L);
        assertThat(position.getLoanAmount()).isEqualTo(210_000L);
    }

    @Test
    @DisplayName("reduceBySell: 나누어떨어지지 않으면 매도분은 내림하고, 나머지는 포지션에 남긴다")
    void reduceBySell_withRemainder() {
        LeveragePositionEntity position = position(3, 100_000L, 100_003L); // 대출 50,000

        long repaid = position.reduceBySell(1);

        // 매도분: 33,333 / 33,334 / 16,666 (내림) → 잔여: 66,667 / 66,669 / 33,334
        assertThat(repaid).isEqualTo(16_666L);
        assertThat(position.getPurchaseAmount()).isEqualTo(66_667L);
        assertThat(position.getCostAmount()).isEqualTo(66_669L);
        assertThat(position.getLoanAmount()).isEqualTo(33_334L);
    }

    @Test
    @DisplayName("reduceBySell: 전량 매도하면 모든 금액이 0이 되고 isEmpty가 true가 된다")
    void reduceBySell_full() {
        LeveragePositionEntity position = position(4, 280_000L, 280_040L);

        long repaid = position.reduceBySell(4);

        assertThat(repaid).isEqualTo(140_000L);
        assertThat(position.getPurchaseAmount()).isZero();
        assertThat(position.getCostAmount()).isZero();
        assertThat(position.getLoanAmount()).isZero();
        assertThat(position.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty: 수량이 남아 있으면 false")
    void isEmpty_false() {
        assertThat(position(1, 70_000L, 70_010L).isEmpty()).isFalse();
    }

    @Test
    @DisplayName("changeMarginStatus, markInterestChargedThrough: 상태·마진콜 일자·청구일을 갱신한다")
    void stateChanges() {
        LeveragePositionEntity position = position(10, 700_000L, 700_100L);
        LocalDate date = LocalDate.of(2026, 9, 21);

        position.changeMarginStatus(MarginStatus.MARGIN_CALL, date);
        position.markInterestChargedThrough(date);

        assertThat(position.getMarginStatus()).isEqualTo(MarginStatus.MARGIN_CALL);
        assertThat(position.getMarginCallDate()).isEqualTo(date);
        assertThat(position.getLastInterestChargedDate()).isEqualTo(date);
    }

    @Test
    @DisplayName("prePersist: 생성·수정 시각을 채우고, 기존 청구일은 유지한다")
    void prePersist_keepsInterestDate() {
        LeveragePositionEntity position = position(10, 700_000L, 700_100L);
        LocalDate lastCharged = LocalDate.of(2026, 9, 1);
        position.setLastInterestChargedDate(lastCharged);

        ReflectionTestUtils.invokeMethod(position, "prePersist");

        assertThat(position.getCreatedDateTime()).isNotNull();
        assertThat(position.getUpdatedDateTime()).isNotNull();
        assertThat(position.getLastInterestChargedDate()).isEqualTo(lastCharged);
    }

    @Test
    @DisplayName("prePersist: 청구일이 비어 있으면 오늘로 채운다")
    void prePersist_fillsInterestDate() {
        LeveragePositionEntity position = position(10, 700_000L, 700_100L);
        position.setLastInterestChargedDate(null);

        ReflectionTestUtils.invokeMethod(position, "prePersist");

        assertThat(position.getLastInterestChargedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("preUpdate: 수정 시각만 갱신한다")
    void preUpdate() {
        LeveragePositionEntity position = position(10, 700_000L, 700_100L);
        Instant created = Instant.parse("2026-09-01T00:00:00Z");
        position.setCreatedDateTime(created);
        position.setUpdatedDateTime(created);

        ReflectionTestUtils.invokeMethod(position, "preUpdate");

        assertThat(position.getCreatedDateTime()).isEqualTo(created);
        assertThat(position.getUpdatedDateTime()).isAfter(created);
    }
}

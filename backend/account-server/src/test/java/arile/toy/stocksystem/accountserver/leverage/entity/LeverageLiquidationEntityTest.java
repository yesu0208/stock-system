package arile.toy.stocksystem.accountserver.leverage.entity;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LeverageLiquidationEntityTest {

    private static LeverageLiquidationEntity entity() {
        return LeverageLiquidationEntity.of(
                "user1", "005930", LeverageRatio.X2, 10, 30_000L, 300_000L, 350_000L, 50_000L);
    }

    @Test
    @DisplayName("of: 청산 수량·정산가·대금·상환 대출금·부족분을 담아 생성한다")
    void of() {
        LeverageLiquidationEntity entity = entity();

        assertThat(entity.getUsername()).isEqualTo("user1");
        assertThat(entity.getStockCode()).isEqualTo("005930");
        assertThat(entity.getLeverageRatio()).isEqualTo(LeverageRatio.X2);
        assertThat(entity.getLiquidatedQuantity()).isEqualTo(10);
        assertThat(entity.getSettlementPrice()).isEqualTo(30_000L);
        assertThat(entity.getProceeds()).isEqualTo(300_000L);
        assertThat(entity.getRepaidLoanAmount()).isEqualTo(350_000L);
        assertThat(entity.getShortfall()).isEqualTo(50_000L);
        assertThat(entity.getLiquidatedAt()).isNull();
    }

    @Test
    @DisplayName("prePersist: 청산 시각을 기록한다")
    void prePersist() {
        LeverageLiquidationEntity entity = entity();

        ReflectionTestUtils.invokeMethod(entity, "prePersist");

        assertThat(entity.getLiquidatedAt()).isNotNull();
    }
}

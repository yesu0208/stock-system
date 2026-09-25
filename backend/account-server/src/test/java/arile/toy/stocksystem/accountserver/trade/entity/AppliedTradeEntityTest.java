package arile.toy.stocksystem.accountserver.trade.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Entity] 반영 완료 체결 기록 테스트")
class AppliedTradeEntityTest {

    @DisplayName("종목코드·체결 ID를 담고, 생성 시점을 반영 시각으로 기록한다")
    @Test
    void givenStockCodeAndTradeId_whenCreating_thenSetsFieldsAndAppliedAt() {
        Instant before = Instant.now();

        AppliedTradeEntity entity = AppliedTradeEntity.of("005930", 100L);

        Instant after = Instant.now();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getStockCode()).isEqualTo("005930");
        assertThat(entity.getTradeId()).isEqualTo(100L);
        assertThat(entity.getAppliedAt()).isBetween(before, after);
    }
}

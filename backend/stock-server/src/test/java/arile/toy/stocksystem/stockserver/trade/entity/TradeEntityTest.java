package arile.toy.stocksystem.stockserver.trade.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Entity] 체결 엔티티 테스트")
class TradeEntityTest {

    @DisplayName("저장 직전에는, 체결 시각을 현재 시각으로 채운다.")
    @Test
    void givenNewTrade_whenPrePersist_thenSetsExecutedAt() {
        TradeEntity sut = new TradeEntity();
        Instant before = Instant.now();

        ReflectionTestUtils.invokeMethod(sut, "prePersist");

        assertThat(sut.getExecutedAt()).isNotNull()
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(Instant.now());
    }
}

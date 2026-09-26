package arile.toy.stocksystem.stockserver.trade.outbox.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[Entity] 체결 아웃박스 엔티티 테스트")
class TradeOutboxEntityTest {

    @DisplayName("저장 직전에는, 생성 시각을 현재 시각으로 채운다.")
    @Test
    void givenNewOutbox_whenPrePersist_thenSetsCreatedDateTime() {
        TradeOutboxEntity sut = TradeOutboxEntity.of("TRADE_EXECUTED", "{}");
        Instant before = Instant.now();

        ReflectionTestUtils.invokeMethod(sut, "prePersist");

        assertThat(sut.getCreatedDateTime()).isNotNull()
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(Instant.now());
        assertThat(sut.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(sut.getPublishedDateTime()).isNull();
    }
}

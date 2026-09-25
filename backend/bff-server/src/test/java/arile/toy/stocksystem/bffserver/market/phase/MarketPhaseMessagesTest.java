package arile.toy.stocksystem.bffserver.market.phase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class MarketPhaseMessagesTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = BffServerMarketPhase.class, names = "CLOSED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("CLOSED가 아닌 장 상태는 모두 주문 가능하다")
    void orderable(BffServerMarketPhase phase) {
        assertThat(phase.isOrderable()).isTrue();
    }

    @Test
    @DisplayName("CLOSED는 주문할 수 없다")
    void closed_notOrderable() {
        assertThat(BffServerMarketPhase.CLOSED.isOrderable()).isFalse();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "MORNING_CALL, 동시호가",
            "OPEN, OPEN",
            "CLOSING_CALL, 동시호가",
            "AFTER, AFTER",
            "CLOSED, CLOSED"
    })
    @DisplayName("장 상태 푸시 메시지는 상태 이름과 화면 표시 라벨을 담는다 (동시호가는 개장·마감 모두 같은 라벨)")
    void phasePushMessage(BffServerMarketPhase phase, String label) {
        MarketPhasePushMessage message = MarketPhasePushMessage.of(phase);

        assertThat(message.phase()).isEqualTo(phase.name());
        assertThat(message.label()).isEqualTo(label);
    }

    @Test
    @DisplayName("장 마감 푸시 메시지: 애프터마켓이면 애프터마켓 정산 안내")
    void closePushMessage_after() {
        MarketClosePushMessage message = MarketClosePushMessage.of("AFTER");

        assertThat(message.session()).isEqualTo("AFTER");
        assertThat(message.message()).isEqualTo("애프터마켓 미체결 내역 정리 및 정산이 완료되었습니다.");
    }

    @Test
    @DisplayName("장 마감 푸시 메시지: 그 외(정규장)면 정규장 정산 안내")
    void closePushMessage_regular() {
        assertThat(MarketClosePushMessage.of("REGULAR").message())
                .isEqualTo("정규장 미체결 내역 정리 및 정산이 완료되었습니다.");
    }
}

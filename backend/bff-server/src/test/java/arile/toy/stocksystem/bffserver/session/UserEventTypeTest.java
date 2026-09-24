package arile.toy.stocksystem.bffserver.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class UserEventTypeTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "ORDER,                user:order.user1:event",
            "TRADE,                user:trade.user1:event",
            "CANCEL,               user:cancel.user1:event",
            "ACCOUNT,              user:account.user1:event",
            "AUTO_ORDER,           user:auto:order.user1:event",
            "AUTO_CANCEL,          user:auto:cancel.user1:event",
            "TRAILING_STOP,        user:trailing:stop.user1:event",
            "TRAILING_STOP_CANCEL, user:trailing:stop:cancel.user1:event",
            "OTOCO,                user:otoco.user1:event",
            "OTOCO_CANCEL,         user:otoco:cancel.user1:event",
            "ALERT,                user:alert.user1:event",
            "ALERT_CANCEL,         user:alert:cancel.user1:event",
            "ALERT_FIRED,          user:alert:fired.user1:event",
            "MARGIN_CALL,          user:margincall.user1:event",
            "LIQUIDATION,          user:liquidation.user1:event",
            "QUEUE_POSITION,       user:order:queue-position.user1:event"
    })
    @DisplayName("이벤트 종류별 사용자 채널 이름 (publisher 쪽 채널 이름과 일치해야 함)")
    void channel(UserEventType type, String expected) {
        assertThat(type.channel("user1")).isEqualTo(expected);
    }

    @Test
    @DisplayName("모든 이벤트 종류의 채널 이름은 서로 겹치지 않는다")
    void channelsAreUnique() {
        long distinct = Arrays.stream(UserEventType.values())
                .map(type -> type.channel("user1"))
                .distinct()
                .count();

        assertThat(distinct).isEqualTo(UserEventType.values().length);
    }
}

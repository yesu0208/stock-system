package arile.toy.stocksystem.bffserver.trade.dto;

import arile.toy.stocksystem.bffserver.trade.event.TradeResponseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TradeResponseTest {

    @Test
    @DisplayName("fromEvent: 체결 이벤트의 모든 필드를 그대로 옮긴다")
    void fromEvent() {
        Instant executedAt = Instant.parse("2026-09-24T00:31:00Z");

        TradeResponse response = TradeResponse.fromEvent(
                new TradeResponseEvent(100L, 1L, "user1", "005930", TradeType.SELL, 71_000, 5, executedAt));

        assertThat(response).isEqualTo(
                new TradeResponse(100L, 1L, "user1", "005930", TradeType.SELL, 71_000, 5, executedAt));
    }
}

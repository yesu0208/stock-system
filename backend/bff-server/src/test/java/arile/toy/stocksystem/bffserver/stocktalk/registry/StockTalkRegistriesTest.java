package arile.toy.stocksystem.bffserver.stocktalk.registry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockTalkRegistriesTest {

    @Nested
    @DisplayName("방 레지스트리")
    class Rooms {

        @Test
        @DisplayName("처음 접근하면 방을 만들고, 대소문자와 관계없이 같은 방을 반환한다")
        void getOrCreate() {
            StockTalkRoomRegistry registry = new StockTalkRoomRegistry();

            assertThat(registry.exists("aapl")).isFalse();
            StockTalkRoom room = registry.getOrCreate("aapl");

            assertThat(registry.getOrCreate("AAPL")).isSameAs(room);
            assertThat(registry.exists("Aapl")).isTrue();
            assertThat(room.getTicker()).isEqualTo("AAPL");
        }
    }

    @Nested
    @DisplayName("세션 레지스트리")
    class Sessions {

        @Test
        @DisplayName("registerJoin: 세션의 첫 입장이면 true, 같은 세션의 같은 종목 재입장이면 false")
        void registerJoin() {
            StockTalkSessionRegistry registry = new StockTalkSessionRegistry();

            assertThat(registry.registerJoin("session-A", "user1", "005930")).isTrue();
            assertThat(registry.registerJoin("session-A", "user1", "005930")).isFalse();
            assertThat(registry.registerJoin("session-A", "user1", "000660")).isTrue();
        }

        @Test
        @DisplayName("registerLeave: 입장해 있던 종목이면 true, 입장한 적 없으면 false")
        void registerLeave() {
            StockTalkSessionRegistry registry = new StockTalkSessionRegistry();
            registry.registerJoin("session-A", "user1", "005930");

            assertThat(registry.registerLeave("session-A", "005930")).isTrue();
            assertThat(registry.registerLeave("session-A", "005930")).isFalse();
            assertThat(registry.registerLeave("session-B", "005930")).isFalse();
        }

        @Test
        @DisplayName("removeSession: 세션의 사용자와 참여 종목을 돌려주고 기록을 지운다")
        void removeSession() {
            StockTalkSessionRegistry registry = new StockTalkSessionRegistry();
            registry.registerJoin("session-A", "user1", "005930");
            registry.registerJoin("session-A", "user1", "000660");

            StockTalkSessionRegistry.SessionParticipation participation = registry.removeSession("session-A");

            assertThat(participation.username()).isEqualTo("user1");
            assertThat(participation.tickers()).containsExactlyInAnyOrder("005930", "000660");
            assertThat(registry.removeSession("session-A")).isNull();
        }

        @Test
        @DisplayName("모든 종목에서 퇴장한 세션은 기록이 지워져, 연결이 끊겨도 자동 퇴장 대상이 없다")
        void allLeft_cleared() {
            StockTalkSessionRegistry registry = new StockTalkSessionRegistry();
            registry.registerJoin("session-A", "user1", "005930");
            registry.registerLeave("session-A", "005930");

            assertThat(registry.removeSession("session-A")).isNull();
        }
    }
}

package arile.toy.stocksystem.bffserver.stocktalk.registry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class StockTalkSessionRegistryTest {

    private final StockTalkSessionRegistry registry = new StockTalkSessionRegistry();

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Set<String>> sessionTickers() {
        return (ConcurrentHashMap<String, Set<String>>) ReflectionTestUtils.getField(registry, "sessionTickers");
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, String> sessionUsername() {
        return (ConcurrentHashMap<String, String>) ReflectionTestUtils.getField(registry, "sessionUsername");
    }

    // ===================== 입장 =====================

    @Nested
    @DisplayName("입장")
    class Join {

        @Test
        @DisplayName("처음 입장하면 true, 같은 세션이 같은 종목에 다시 입장하면 false (방 인원이 두 번 늘지 않도록)")
        void firstJoinOnly() {
            assertThat(registry.registerJoin("session-A", "user1", "005930")).isTrue();
            assertThat(registry.registerJoin("session-A", "user1", "005930")).isFalse();
        }

        @Test
        @DisplayName("종목코드는 대소문자를 구분하지 않는다")
        void tickerCaseInsensitive() {
            assertThat(registry.registerJoin("session-A", "user1", "aapl")).isTrue();
            assertThat(registry.registerJoin("session-A", "user1", "AAPL")).isFalse();
        }

        @Test
        @DisplayName("같은 사용자라도 다른 세션(다른 탭)이면 각자 입장으로 기록한다")
        void differentSessions() {
            assertThat(registry.registerJoin("session-A", "user1", "005930")).isTrue();
            assertThat(registry.registerJoin("session-B", "user1", "005930")).isTrue();
        }
    }

    // ===================== 퇴장 =====================

    @Nested
    @DisplayName("퇴장")
    class Leave {

        @Test
        @DisplayName("입장해 있던 종목에서 나가면 true, 입장한 적 없는 종목이면 false (방 인원이 음수가 되지 않도록)")
        void leaveOnlyIfJoined() {
            registry.registerJoin("session-A", "user1", "005930");

            assertThat(registry.registerLeave("session-A", "000660")).isFalse();
            assertThat(registry.registerLeave("session-A", "005930")).isTrue();
            assertThat(registry.registerLeave("session-A", "005930")).isFalse();
        }

        @Test
        @DisplayName("입장 기록이 없는 세션이 나가면 false")
        void unknownSession() {
            assertThat(registry.registerLeave("session-X", "005930")).isFalse();
        }

        @Test
        @DisplayName("마지막 종목에서 나가면 세션 기록을 모두 지운다")
        void leaveLast_clearsSession() {
            registry.registerJoin("session-A", "user1", "005930");

            registry.registerLeave("session-A", "005930");

            assertThat(sessionTickers()).doesNotContainKey("session-A");
            assertThat(sessionUsername()).doesNotContainKey("session-A");
            assertThat(registry.removeSession("session-A")).isNull();
        }

        @Test
        @DisplayName("여러 종목에 입장한 세션이 한 종목만 나가면, 남은 종목의 참여 기록은 유지한다 (연결 종료 시 자동 퇴장 대상)")
        void leaveOneOfMany_keepsRemaining() {
            registry.registerJoin("session-A", "user1", "005930");
            registry.registerJoin("session-A", "user1", "000660");

            assertThat(registry.registerLeave("session-A", "005930")).isTrue();

            StockTalkSessionRegistry.SessionParticipation participation = registry.removeSession("session-A");
            assertThat(participation.username()).isEqualTo("user1");
            assertThat(participation.tickers()).containsExactly("000660");
        }
    }

    // ===================== 연결 종료 =====================

    @Nested
    @DisplayName("연결 종료")
    class RemoveSession {

        @Test
        @DisplayName("입장해 있던 사용자와 모든 종목(대문자)을 돌려주고 세션 기록을 지운다 (자동 퇴장용)")
        void returnsParticipation() {
            registry.registerJoin("session-A", "user1", "005930");
            registry.registerJoin("session-A", "user1", "aapl");

            StockTalkSessionRegistry.SessionParticipation participation = registry.removeSession("session-A");

            assertThat(participation.username()).isEqualTo("user1");
            assertThat(participation.tickers()).containsExactlyInAnyOrder("005930", "AAPL");
            assertThat(registry.removeSession("session-A")).isNull();
        }

        @Test
        @DisplayName("종목톡에 입장한 적 없는 세션이면 null")
        void neverJoined() {
            assertThat(registry.removeSession("session-X")).isNull();
        }

        @Test
        @DisplayName("[동시 처리 방어] 종목 목록이 빈 상태로 남아 있으면 퇴장할 종목이 없다(null)")
        void emptyTickers_null() {
            // 입장 처리가 빈 목록을 만든 직후 종목을 넣기 전에 연결 종료가 처리된 순간의 상태
            sessionTickers().put("session-A", ConcurrentHashMap.newKeySet());
            sessionUsername().put("session-A", "user1");

            assertThat(registry.removeSession("session-A")).isNull();
        }

        @Test
        @DisplayName("[동시 처리 방어] 사용자 이름 기록이 없으면 퇴장 처리하지 않는다(null)")
        void noUsername_null() {
            // 퇴장 처리가 사용자 이름을 지운 직후 같은 목록에 종목이 다시 들어간 순간의 상태
            Set<String> tickers = ConcurrentHashMap.newKeySet();
            tickers.add("005930");
            sessionTickers().put("session-A", tickers);

            assertThat(registry.removeSession("session-A")).isNull();
        }
    }
}

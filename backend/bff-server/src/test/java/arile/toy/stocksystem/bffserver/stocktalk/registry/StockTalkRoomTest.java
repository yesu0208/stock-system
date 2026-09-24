package arile.toy.stocksystem.bffserver.stocktalk.registry;

import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTalkRoomTest {

    private static StockTalkMessage chat(String content) {
        return StockTalkMessage.chat("005930", "user1", "닉네임", null, content, 1);
    }

    @Nested
    @DisplayName("참여")
    class Participation {

        @Test
        @DisplayName("사용자의 첫 입장이면 true, 같은 사용자가 또 들어오면 false이고 참여자 수는 사용자 기준이다")
        void join() {
            StockTalkRoom room = new StockTalkRoom("005930");

            assertThat(room.join("user1")).isTrue();
            assertThat(room.join("user1")).isFalse();
            assertThat(room.join("user2")).isTrue();

            assertThat(room.participantCount()).isEqualTo(2);
            assertThat(room.isParticipant("user1")).isTrue();
        }

        @Test
        @DisplayName("입장 횟수만큼 퇴장해야 완전히 나가며, 그때만 true를 반환한다")
        void leave() {
            StockTalkRoom room = new StockTalkRoom("005930");
            room.join("user1");
            room.join("user1");

            assertThat(room.leave("user1")).isFalse();
            assertThat(room.isParticipant("user1")).isTrue();

            assertThat(room.leave("user1")).isTrue();
            assertThat(room.isParticipant("user1")).isFalse();
            assertThat(room.participantCount()).isZero();
        }

        @Test
        @DisplayName("참여하지 않은 사용자의 퇴장은 false를 반환한다")
        void leaveWithoutJoin() {
            assertThat(new StockTalkRoom("005930").leave("user1")).isFalse();
        }
    }

    @Nested
    @DisplayName("메시지")
    class Messages {

        @Test
        @DisplayName("최근 N개를 오래된 순으로 반환하고, 전체가 N보다 적으면 모두 반환한다")
        void recentMessages() {
            StockTalkRoom room = new StockTalkRoom("005930");
            room.addMessage(chat("1"));
            room.addMessage(chat("2"));
            room.addMessage(chat("3"));

            assertThat(room.getRecentMessages(2)).extracting(StockTalkMessage::content).containsExactly("2", "3");
            assertThat(room.getRecentMessages(10)).extracting(StockTalkMessage::content).containsExactly("1", "2", "3");
        }

        @Test
        @DisplayName("최근 메시지는 복사본이라, 이후 메시지가 추가돼도 읽는 중 예외가 나지 않고 수정할 수 없다")
        void recentMessages_isSnapshot() {
            StockTalkRoom room = new StockTalkRoom("005930");
            room.addMessage(chat("1"));

            List<StockTalkMessage> recent = room.getRecentMessages(50);
            room.addMessage(chat("2"));

            assertThat(recent).extracting(StockTalkMessage::content).containsExactly("1");
            assertThatThrownBy(() -> recent.add(chat("x"))).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("200개를 넘으면 오래된 메시지부터 지워 최근 200개만 유지한다")
        void maxHistory() {
            StockTalkRoom room = new StockTalkRoom("005930");
            for (int i = 1; i <= 205; i++) {
                room.addMessage(chat(String.valueOf(i)));
            }

            assertThat(room.getAllMessages()).hasSize(200);
            assertThat(room.getAllMessages().get(0).content()).isEqualTo("6");
            assertThat(room.getAllMessages().get(199).content()).isEqualTo("205");
        }
    }
}

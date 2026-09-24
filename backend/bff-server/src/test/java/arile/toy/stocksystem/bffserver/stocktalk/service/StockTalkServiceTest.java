package arile.toy.stocksystem.bffserver.stocktalk.service;

import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkJoinResponse;
import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkMessage;
import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkMessageType;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkRoom;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkRoomRegistry;
import arile.toy.stocksystem.bffserver.user.dto.UserProfile;
import arile.toy.stocksystem.bffserver.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StockTalkServiceTest {

    private StockTalkRoomRegistry roomRegistry;
    private SimpMessagingTemplate messagingTemplate;
    private StockTalkService service;

    @BeforeEach
    void setUp() {
        roomRegistry = new StockTalkRoomRegistry();
        messagingTemplate = mock(SimpMessagingTemplate.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        UserProfile profile = mock(UserProfile.class);
        given(profile.nickname()).willReturn("닉네임");
        given(profile.profileImageUrl()).willReturn("/uploads/profile/user1.png");
        given(userProfileService.getProfile(anyString())).willReturn(profile);

        service = new StockTalkService(roomRegistry, messagingTemplate, userProfileService);
    }

    private StockTalkMessage lastBroadcast() {
        ArgumentCaptor<StockTalkMessage> captor = ArgumentCaptor.forClass(StockTalkMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/stock-talk/005930"), captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("입장")
    class Join {

        @Test
        @DisplayName("입장한 세션에게만 참여자 수·최근 메시지를 보내고, 처음 들어온 사용자면 입장 알림을 방 전체에 보낸다")
        @SuppressWarnings("unchecked")
        void firstJoin() {
            service.join("005930", "user1", "session-A");

            ArgumentCaptor<Map<String, Object>> headers = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<StockTalkJoinResponse> history = ArgumentCaptor.forClass(StockTalkJoinResponse.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq("user1"), eq("/sub/stock-talk/history"), history.capture(), headers.capture());
            assertThat(SimpMessageHeaderAccessor.getSessionId(headers.getValue())).isEqualTo("session-A");
            assertThat(history.getValue().participantCount()).isEqualTo(1);

            StockTalkMessage enter = lastBroadcast();
            assertThat(enter.type()).isEqualTo(StockTalkMessageType.ENTER);
            assertThat(enter.content()).isEqualTo("닉네임님이 입장하셨습니다.");
            assertThat(roomRegistry.getOrCreate("005930").getAllMessages()).containsExactly(enter);
        }

        @Test
        @DisplayName("이미 참여 중인 사용자가 다른 탭으로 들어오면 최근 메시지만 보내고 입장 알림은 보내지 않는다")
        void alreadyParticipating_noEnterMessage() {
            service.join("005930", "user1", "session-A");
            clearInvocations(messagingTemplate);

            service.join("005930", "user1", "session-B");

            verify(messagingTemplate).convertAndSendToUser(
                    eq("user1"), eq("/sub/stock-talk/history"), any(StockTalkJoinResponse.class), any(Map.class));
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("sendHistory: 참여 카운트를 바꾸지 않고 최근 메시지만 다시 보낸다")
        void sendHistory() {
            service.join("005930", "user1", "session-A");
            clearInvocations(messagingTemplate);

            service.sendHistory("005930", "user1", "session-A");

            verify(messagingTemplate).convertAndSendToUser(
                    eq("user1"), eq("/sub/stock-talk/history"), any(StockTalkJoinResponse.class), any(Map.class));
            assertThat(roomRegistry.getOrCreate("005930").leave("user1")).isTrue();
        }
    }

    @Nested
    @DisplayName("퇴장")
    class Leave {

        @Test
        @DisplayName("사용자가 완전히 나가면 퇴장 알림을 방 전체에 보낸다")
        void fullyLeft() {
            service.join("005930", "user1", "session-A");
            clearInvocations(messagingTemplate);

            service.leave("005930", "user1");

            StockTalkMessage leave = lastBroadcast();
            assertThat(leave.type()).isEqualTo(StockTalkMessageType.LEAVE);
            assertThat(leave.content()).isEqualTo("닉네임님이 퇴장하셨습니다.");
            assertThat(leave.participantCount()).isZero();
        }

        @Test
        @DisplayName("다른 탭이 남아 있거나 참여하지 않은 사용자면 퇴장 알림을 보내지 않는다")
        void notFullyLeft_noMessage() {
            service.join("005930", "user1", "session-A");
            service.join("005930", "user1", "session-B");
            clearInvocations(messagingTemplate);

            service.leave("005930", "user1");
            service.leave("005930", "user2");

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("채팅")
    class SendMessage {

        @Test
        @DisplayName("참여자의 채팅을 방에 저장하고 대문자 종목 채널로 방 전체에 보낸다")
        void participant() {
            service.join("005930", "user1", "session-A");
            clearInvocations(messagingTemplate);

            service.sendMessage("005930", "user1", "오늘 많이 오르네요");

            StockTalkMessage chat = lastBroadcast();
            assertThat(chat.type()).isEqualTo(StockTalkMessageType.CHAT);
            assertThat(chat.sender()).isEqualTo("user1");
            assertThat(chat.senderNickname()).isEqualTo("닉네임");
            assertThat(chat.content()).isEqualTo("오늘 많이 오르네요");
            assertThat(roomRegistry.getOrCreate("005930").getAllMessages()).endsWith(chat);
        }

        @Test
        @DisplayName("참여하지 않은 사용자의 채팅은 보내지도 저장하지도 않는다")
        void nonParticipant_rejected() {
            service.sendMessage("005930", "user1", "몰래 보내기");

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
            assertThat(roomRegistry.getOrCreate("005930").getAllMessages()).isEmpty();
        }
    }
}

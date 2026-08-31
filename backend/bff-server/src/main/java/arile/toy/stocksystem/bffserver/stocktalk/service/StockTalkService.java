package arile.toy.stocksystem.bffserver.stocktalk.service;

import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkJoinResponse;
import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkMessage;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkRoom;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkRoomRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTalkService {

    private static final int HISTORY_SEND_COUNT = 50; // 입장 시 최근 N개 전송
    private static final String TOPIC_PREFIX = "/sub/stock-talk/";
    private static final String HISTORY_DESTINATION = "/sub/stock-talk/history";

    private final StockTalkRoomRegistry roomRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 1) 방 참여자 목록에 추가
     * 2) 입장한 사용자에게만 "히스토리 + 현재 참여자 수"를 함께 전송한다.
     *    (참여자 수를 별도로 내려주지 않으면 프론트가 히스토리 속 과거 ENTER/LEAVE를
     *     재처리하며 카운트를 잘못 추정하게 되므로, 여기서 정확한 수치를 명시적으로 계산해 전달한다.)
     * 3) 방 전체에 ENTER 메시지 브로드캐스트 (신규 입장자인 경우에만)
     */
    public void join(String ticker, String username) {
        StockTalkRoom room = roomRegistry.getOrCreate(ticker);
        boolean isNew = room.join(username);

        StockTalkJoinResponse response = StockTalkJoinResponse.of(
                room.participantCount(),
                room.getRecentMessages(HISTORY_SEND_COUNT)
        );

        messagingTemplate.convertAndSendToUser(username, HISTORY_DESTINATION, response);

        if (isNew) {
            StockTalkMessage enterMsg = StockTalkMessage.enter(ticker, username, room.participantCount());
            room.addMessage(enterMsg);
            broadcast(ticker, enterMsg);
            log.info("[StockTalk] {} joined {}, participants={}", username, ticker, room.participantCount());
        }
    }

    public void leave(String ticker, String username) {
        StockTalkRoom room = roomRegistry.getOrCreate(ticker);
        boolean wasIn = room.leave(username);

        if (wasIn) {
            StockTalkMessage leaveMsg = StockTalkMessage.leave(ticker, username, room.participantCount());
            room.addMessage(leaveMsg);
            broadcast(ticker, leaveMsg);
            log.info("[StockTalk] {} left {}, participants={}", username, ticker, room.participantCount());
        }
    }

    public void sendMessage(String ticker, String username, String content) {
        StockTalkRoom room = roomRegistry.getOrCreate(ticker);

        if (!room.isParticipant(username)) {
            log.warn("[StockTalk] {} tried to send message to {} without joining", username, ticker);
            return;
        }

        StockTalkMessage msg = StockTalkMessage.chat(ticker, username, content, room.participantCount());
        room.addMessage(msg);
        broadcast(ticker, msg);
    }

    private void broadcast(String ticker, StockTalkMessage message) {
        messagingTemplate.convertAndSend(TOPIC_PREFIX + ticker.toUpperCase(), message);
    }
}

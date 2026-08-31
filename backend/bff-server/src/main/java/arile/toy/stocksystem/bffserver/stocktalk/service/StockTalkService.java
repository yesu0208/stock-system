package arile.toy.stocksystem.bffserver.stocktalk.service;

import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkJoinResponse;
import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkMessage;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkRoom;
import arile.toy.stocksystem.bffserver.stocktalk.registry.StockTalkRoomRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
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

    public void join(String ticker, String username, String sessionId) {
        StockTalkRoom room = roomRegistry.getOrCreate(ticker);
        boolean isNew = room.join(username);

        StockTalkJoinResponse response = StockTalkJoinResponse.of(
                ticker,
                room.participantCount(),
                room.getRecentMessages(HISTORY_SEND_COUNT)
        );

        sendToSession(username, sessionId, response);

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

    private void sendToSession(String username, String sessionId, StockTalkJoinResponse response) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(
                username,
                HISTORY_DESTINATION,
                response,
                headerAccessor.getMessageHeaders()
        );
    }
}

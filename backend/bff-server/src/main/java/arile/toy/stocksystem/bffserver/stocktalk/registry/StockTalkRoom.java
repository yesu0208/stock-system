package arile.toy.stocksystem.bffserver.stocktalk.registry;

import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkMessage;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 종목별 톡방 (인메모리).
 * - participants : 현재 참여 중인 username 집합
 * - messages     : 누적 메시지 (최대 MAX_HISTORY 건 유지)
 */
@Getter
public class StockTalkRoom {

    private static final int MAX_HISTORY = 200;

    private final String ticker;
    private final Set<String> participants = ConcurrentHashMap.newKeySet();
    private final List<StockTalkMessage> messages = new CopyOnWriteArrayList<>();

    public StockTalkRoom(String ticker) {
        this.ticker = ticker;
    }

    public boolean join(String username) {
        return participants.add(username);
    }

    public boolean leave(String username) {
        return participants.remove(username);
    }

    public boolean isParticipant(String username) {
        return participants.contains(username);
    }

    public int participantCount() {
        return participants.size();
    }

    public void addMessage(StockTalkMessage message) {
        messages.add(message);
        if (messages.size() > MAX_HISTORY) {
            messages.subList(0, messages.size() - MAX_HISTORY).clear();
        }
    }

    /** 최근 n 건 반환 */
    public List<StockTalkMessage> getRecentMessages(int n) {
        int size = messages.size();
        int from = Math.max(0, size - n);
        return Collections.unmodifiableList(messages.subList(from, size));
    }

    public List<StockTalkMessage> getAllMessages() {
        return Collections.unmodifiableList(messages);
    }
}

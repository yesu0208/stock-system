package arile.toy.stocksystem.bffserver.stocktalk.registry;

import arile.toy.stocksystem.bffserver.stocktalk.dto.StockTalkMessage;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class StockTalkRoom {

    private static final int MAX_HISTORY = 200;

    private final String ticker;
    private final ConcurrentHashMap<String, AtomicInteger> participantRefCounts = new ConcurrentHashMap<>();
    private final List<StockTalkMessage> messages = new CopyOnWriteArrayList<>();

    public StockTalkRoom(String ticker) {
        this.ticker = ticker;
    }

    public boolean join(String username) {
        AtomicInteger[] isNewHolder = new AtomicInteger[1];

        participantRefCounts.compute(username, (user, count) -> {
            if (count == null) {
                isNewHolder[0] = new AtomicInteger(1);
                return new AtomicInteger(1);
            }
            count.incrementAndGet();
            return count;
        });

        return isNewHolder[0] != null;
    }

    public boolean leave(String username) {
        boolean[] fullyLeftHolder = new boolean[1];

        participantRefCounts.computeIfPresent(username, (user, count) -> {
            if (count.decrementAndGet() <= 0) {
                fullyLeftHolder[0] = true;
                return null;
            }
            return count;
        });

        return fullyLeftHolder[0];
    }

    public boolean isParticipant(String username) {
        return participantRefCounts.containsKey(username);
    }

    public int participantCount() {
        return participantRefCounts.size();
    }

    public void addMessage(StockTalkMessage message) {
        messages.add(message);
        if (messages.size() > MAX_HISTORY) {
            messages.subList(0, messages.size() - MAX_HISTORY).clear();
        }
    }

    public List<StockTalkMessage> getRecentMessages(int n) {
        int size = messages.size();
        int from = Math.max(0, size - n);
        return Collections.unmodifiableList(messages.subList(from, size));
    }

    public List<StockTalkMessage> getAllMessages() {
        return Collections.unmodifiableList(messages);
    }
}

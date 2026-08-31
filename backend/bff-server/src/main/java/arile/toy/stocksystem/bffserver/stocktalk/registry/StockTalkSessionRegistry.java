package arile.toy.stocksystem.bffserver.stocktalk.registry;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockTalkSessionRegistry {

    private final ConcurrentHashMap<String, Set<String>> sessionTickers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionUsername = new ConcurrentHashMap<>();

    public void registerJoin(String sessionId, String username, String ticker) {
        sessionUsername.putIfAbsent(sessionId, username);
        sessionTickers
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(ticker.toUpperCase());
    }

    public void registerLeave(String sessionId, String ticker) {
        Set<String> tickers = sessionTickers.get(sessionId);
        if (tickers == null) return;

        tickers.remove(ticker.toUpperCase());

        if (tickers.isEmpty()) {
            sessionTickers.remove(sessionId);
            sessionUsername.remove(sessionId);
        }
    }

    public SessionParticipation removeSession(String sessionId) {
        Set<String> tickers = sessionTickers.remove(sessionId);
        String username = sessionUsername.remove(sessionId);

        if (tickers == null || tickers.isEmpty() || username == null) {
            return null;
        }
        return new SessionParticipation(username, tickers);
    }

    public record SessionParticipation(String username, Set<String> tickers) {
    }
}

package arile.toy.stocksystem.bffserver.stocktalk.registry;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockTalkSessionRegistry {

    private final ConcurrentHashMap<String, Set<String>> sessionTickers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionUsername = new ConcurrentHashMap<>();

    /** @return 이 세션이 해당 종목에 처음 입장했으면 true (이미 입장해 있으면 false) */
    public boolean registerJoin(String sessionId, String username, String ticker) {
        sessionUsername.putIfAbsent(sessionId, username);
        return sessionTickers
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(ticker.toUpperCase());
    }

    /** @return 이 세션이 해당 종목에 입장해 있었으면 true (입장한 적 없으면 false) */
    public boolean registerLeave(String sessionId, String ticker) {
        Set<String> tickers = sessionTickers.get(sessionId);
        if (tickers == null) return false;

        boolean removed = tickers.remove(ticker.toUpperCase());

        if (tickers.isEmpty()) {
            sessionTickers.remove(sessionId);
            sessionUsername.remove(sessionId);
        }
        return removed;
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

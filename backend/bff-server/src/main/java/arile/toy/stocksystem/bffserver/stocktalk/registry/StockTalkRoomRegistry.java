package arile.toy.stocksystem.bffserver.stocktalk.registry;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 종목 코드(ticker)를 키로 하는 톡방 레지스트리.
 * 최초 접근 시 방이 자동 생성된다.
 */
@Component
public class StockTalkRoomRegistry {

    private final ConcurrentHashMap<String, StockTalkRoom> rooms = new ConcurrentHashMap<>();

    public StockTalkRoom getOrCreate(String ticker) {
        return rooms.computeIfAbsent(normalize(ticker), StockTalkRoom::new);
    }

    public boolean exists(String ticker) {
        return rooms.containsKey(normalize(ticker));
    }

    private String normalize(String ticker) {
        return ticker.toUpperCase();
    }
}

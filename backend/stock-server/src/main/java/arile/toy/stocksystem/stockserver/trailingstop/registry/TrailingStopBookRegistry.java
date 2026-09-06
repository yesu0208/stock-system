package arile.toy.stocksystem.stockserver.trailingstop.registry;

import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TrailingStopBookRegistry {

    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, TrailingStopDto>> books = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Long, TrailingStopDto> book(String stockCode) {
        return books.computeIfAbsent(stockCode, key -> new ConcurrentHashMap<>());
    }

    public void register(TrailingStopDto dto) {
        book(dto.stockCode()).put(dto.trailingStopId(), dto);
    }

    public void update(TrailingStopDto dto) {
        book(dto.stockCode()).put(dto.trailingStopId(), dto);
    }

    public void remove(String stockCode, Long trailingStopId) {
        book(stockCode).remove(trailingStopId);
    }

    public Collection<TrailingStopDto> getAll(String stockCode) {
        return book(stockCode).values();
    }
}

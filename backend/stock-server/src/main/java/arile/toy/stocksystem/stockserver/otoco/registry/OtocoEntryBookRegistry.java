package arile.toy.stocksystem.stockserver.otoco.registry;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtocoEntryBookRegistry {

    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, OtocoDto>> books = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Long, OtocoDto> book(String stockCode) {
        return books.computeIfAbsent(stockCode, key -> new ConcurrentHashMap<>());
    }

    public void register(OtocoDto dto) {
        book(dto.stockCode()).put(dto.otocoId(), dto);
    }

    public void remove(String stockCode, Long otocoId) {
        book(stockCode).remove(otocoId);
    }

    public Collection<OtocoDto> getAll(String stockCode) {
        return book(stockCode).values();
    }
}

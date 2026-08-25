package arile.toy.stocksystem.bffserver.sharding;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StockGroupRegistry {

    private final StockGroupProperties stockGroupProperties;

    private final Map<String, String> stockCodeToGroup = new HashMap<>();

    @PostConstruct
    public void init() {
        stockGroupProperties.getGroups().forEach((group, stockCodes) ->
                stockCodes.forEach(stockCode ->
                        stockCodeToGroup.put(stockCode, group)
                )
        );
    }

    public String resolveGroup(String stockCode) {
        String group = stockCodeToGroup.get(stockCode);
        if (group == null) {
            throw new IllegalStateException("No group mapped for stockCode: " + stockCode);
        }
        return group;
    }
}
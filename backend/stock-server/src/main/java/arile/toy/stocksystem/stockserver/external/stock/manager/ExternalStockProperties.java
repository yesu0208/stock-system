package arile.toy.stocksystem.stockserver.external.stock.manager;

import arile.toy.stocksystem.stockserver.sharding.StockGroupProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExternalStockProperties {

    private final StockGroupProperties stockGroupProperties;

    @Value("${server.group}")
    private String myGroup;

    public List<String> getOpen() {
        return stockGroupProperties.getGroups().getOrDefault(myGroup, List.of());
    }

    public List<String> getClose() {
        return stockGroupProperties.getGroups().getOrDefault("CLOSE", List.of());
    }
}
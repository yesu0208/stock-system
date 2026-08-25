package arile.toy.stocksystem.bffserver.sharding;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCancelStreamShardResolver {

    private final StockGroupRegistry stockGroupRegistry;

    @Value("${redis.streams.cancel.prefix}")
    private String prefix;

    public String resolveStreamKey(String stockCode) {
        String group = stockGroupRegistry.resolveGroup(stockCode);
        return prefix + "-" + group;
    }
}
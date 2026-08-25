package arile.toy.stocksystem.bffserver.sharding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisAutoOrderStreamShardResolver {

    @Value("${redis.streams.auto-order.prefix}")
    private String prefix;

    @Value("${redis.streams.auto-order.shard-count}")
    private int shardCount;

    public String resolveStreamKey(String stockCode) {
        int shard = Math.abs(stockCode.hashCode()) % shardCount;
        return prefix + "-" + shard;
    }
}

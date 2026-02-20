package arile.toy.stocksystem.bffserver.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class RedisAutoOrderStreamShardResolverTest {

    private RedisAutoOrderStreamShardResolver resolver;

    @BeforeEach
    void setup() {
        resolver = new RedisAutoOrderStreamShardResolver();
        ReflectionTestUtils.setField(resolver, "prefix", "auto-order");
        ReflectionTestUtils.setField(resolver, "shardCount", 4);
    }

    @Test
    @DisplayName("단일 종목 코드로 shard key 계산")
    void givenStockCode_whenResolveStreamKey_thenReturnsShardKeyWithinRange() {
        // Given
        String stockCode = "005930";

        // When
        String streamKey = resolver.resolveStreamKey(stockCode);

        // Then
        assertTrue(streamKey.matches("auto-order-[0-3]"), "Shard key should be in range 0-3");
    }

    @Test
    @DisplayName("다른 종목 코드로 shard key 계산 시 null 아님")
    void givenTwoDifferentStockCodes_whenResolveStreamKey_thenBothKeysAreNotNull() {
        // Given
        String stock1 = "005930";
        String stock2 = "000660";

        // When
        String key1 = resolver.resolveStreamKey(stock1);
        String key2 = resolver.resolveStreamKey(stock2);

        // Then
        assertNotNull(key1, "Stream key1 should not be null");
        assertNotNull(key2, "Stream key2 should not be null");
    }

    @Test
    @DisplayName("여러 종목 코드로 shard key 계산 시 항상 shard 범위 내")
    void givenMultipleStockCodes_whenResolveStreamKey_thenShardsAreWithinConfiguredRange() {
        // Given
        int shardCount = 4;

        // When & Then
        for (int i = 0; i < 1000; i++) {
            String stockCode = "STOCK" + i;

            // When
            String key = resolver.resolveStreamKey(stockCode);

            String shardStr = key.substring(key.lastIndexOf("-") + 1);
            int shard = Integer.parseInt(shardStr);

            // Then
            assertTrue(shard >= 0 && shard < shardCount,
                    "Shard should be within range 0-" + (shardCount - 1) + " but got " + shard + " for stockCode=" + stockCode);
        }
    }
}

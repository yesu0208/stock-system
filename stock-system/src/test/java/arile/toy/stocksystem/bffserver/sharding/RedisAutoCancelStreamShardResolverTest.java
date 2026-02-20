package arile.toy.stocksystem.bffserver.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisAutoCancelStreamShardResolverTest {

    private RedisAutoCancelStreamShardResolver resolver;

    @BeforeEach
    void setup() {
        resolver = new RedisAutoCancelStreamShardResolver();
        ReflectionTestUtils.setField(resolver, "prefix", "auto-cancel");
        ReflectionTestUtils.setField(resolver, "shardCount", 4);
    }

    @Test
    @DisplayName("단일 종목 코드로 shard key 계산")
    void givenStockCode_whenResolveStreamKey_thenReturnsShardKeyWithinRange() {
        // given
        String stockCode = "005930";

        // when
        String streamKey = resolver.resolveStreamKey(stockCode);

        // then
        assertTrue(streamKey.matches("auto-cancel-[0-3]"),
                "Shard key should be in range 0-3");
    }

    @Test
    @DisplayName("다른 종목 코드로 shard key 계산 시 null 아님")
    void givenDifferentStockCodes_whenResolveStreamKey_thenReturnsNonNullKeys() {
        // given
        String stock1 = "005930";
        String stock2 = "000660";

        // when
        String key1 = resolver.resolveStreamKey(stock1);
        String key2 = resolver.resolveStreamKey(stock2);

        // then
        assertNotNull(key1, "Stream key for stock1 should not be null");
        assertNotNull(key2, "Stream key for stock2 should not be null");
    }

    @Test
    @DisplayName("여러 종목 코드로 shard key 계산 시 항상 shard 범위 내")
    void givenMultipleStockCodes_whenResolveStreamKey_thenShardInCorrectRange() {
        // given & when & then
        for (int i = 0; i < 1000; i++) {
            String stockCode = "STOCK" + i;
            String key = resolver.resolveStreamKey(stockCode);

            // shard 번호 추출
            String shardStr = key.substring(key.lastIndexOf("-") + 1);
            int shard = Integer.parseInt(shardStr);

            assertTrue(shard >= 0 && shard < 4,
                    "Shard must be within 0 and shardCount-1, got: " + shard + " for stockCode=" + stockCode);
        }
    }
}

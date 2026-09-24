package arile.toy.stocksystem.bffserver.market.phase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BffServerMarketPhaseRegistryTest {

    private static final String SNAPSHOT_KEY = "market:phase:snapshot";
    private static final String GLOBAL_SNAPSHOT_KEY = "market:global-phase:snapshot";

    private HashOperations<String, Object, Object> hashOps;
    private ValueOperations<String, String> valueOps;
    private BffServerMarketPhaseRegistry registry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        hashOps = mock(HashOperations.class);
        valueOps = mock(ValueOperations.class);
        doReturn(hashOps).when(redisTemplate).opsForHash();
        doReturn(valueOps).when(redisTemplate).opsForValue();
        given(hashOps.entries(SNAPSHOT_KEY)).willReturn(Map.of());
        registry = new BffServerMarketPhaseRegistry(redisTemplate);
    }

    // ===================== 조회 =====================

    @Nested
    @DisplayName("종목별 장 상태 조회")
    class Query {

        @Test
        @DisplayName("상태를 모르는 종목은 닫힌 것으로 보고 주문할 수 없다")
        void unknownStock() {
            assertThat(registry.getPhase("005930")).isNull();
            assertThat(registry.isClosed("005930")).isTrue();
            assertThat(registry.isOrderable("005930")).isFalse();
        }

        @Test
        @DisplayName("CLOSED면 닫혀 있고 주문할 수 없다")
        void closed() {
            registry.setPhase("005930", BffServerMarketPhase.CLOSED);

            assertThat(registry.isClosed("005930")).isTrue();
            assertThat(registry.isOrderable("005930")).isFalse();
        }

        @Test
        @DisplayName("동시호가·장중·애프터마켓이면 열려 있고 주문할 수 있다")
        void open() {
            registry.setPhase("005930", BffServerMarketPhase.CLOSING_CALL);

            assertThat(registry.getPhase("005930")).isEqualTo(BffServerMarketPhase.CLOSING_CALL);
            assertThat(registry.isClosed("005930")).isFalse();
            assertThat(registry.isOrderable("005930")).isTrue();
        }

        @Test
        @DisplayName("전체 장 상태는 기본 CLOSED이고, 설정한 값으로 바뀐다")
        void globalPhase() {
            assertThat(registry.getGlobalPhase()).isEqualTo(BffServerMarketPhase.CLOSED);

            registry.setGlobalPhase(BffServerMarketPhase.OPEN);

            assertThat(registry.getGlobalPhase()).isEqualTo(BffServerMarketPhase.OPEN);
        }
    }

    // ===================== 재동기화 =====================

    @Nested
    @DisplayName("Redis 스냅샷 재동기화")
    class Resync {

        @Test
        @DisplayName("시작 시 종목별·전체 장 상태 스냅샷을 불러온다")
        void init_loadsSnapshots() {
            given(hashOps.entries(SNAPSHOT_KEY)).willReturn(Map.of("005930", "OPEN", "000660", "CLOSED"));
            given(valueOps.get(GLOBAL_SNAPSHOT_KEY)).willReturn("OPEN");

            registry.init();

            assertThat(registry.getPhase("005930")).isEqualTo(BffServerMarketPhase.OPEN);
            assertThat(registry.getPhase("000660")).isEqualTo(BffServerMarketPhase.CLOSED);
            assertThat(registry.getGlobalPhase()).isEqualTo(BffServerMarketPhase.OPEN);
        }

        @Test
        @DisplayName("알 수 없는 장 상태 값은 건너뛰고 나머지 종목은 반영한다")
        void unknownPhase_skipped() {
            given(hashOps.entries(SNAPSHOT_KEY)).willReturn(Map.of("005930", "LUNCH", "000660", "OPEN"));

            registry.resync();

            assertThat(registry.getPhase("005930")).isNull();
            assertThat(registry.getPhase("000660")).isEqualTo(BffServerMarketPhase.OPEN);
        }

        @Test
        @DisplayName("전체 장 상태 스냅샷이 없으면 기존 값을 유지한다")
        void globalSnapshotMissing_keepsCurrent() {
            registry.setGlobalPhase(BffServerMarketPhase.AFTER);
            given(valueOps.get(GLOBAL_SNAPSHOT_KEY)).willReturn(null);

            registry.resync();

            assertThat(registry.getGlobalPhase()).isEqualTo(BffServerMarketPhase.AFTER);
        }

        @Test
        @DisplayName("Redis 조회가 실패해도 예외를 던지지 않고 기존 값을 유지한다 (다음 주기에 재시도)")
        void redisFails_keepsCurrent() {
            registry.setPhase("005930", BffServerMarketPhase.OPEN);
            registry.setGlobalPhase(BffServerMarketPhase.OPEN);
            given(hashOps.entries(SNAPSHOT_KEY)).willThrow(new RedisConnectionFailureException("down"));
            given(valueOps.get(GLOBAL_SNAPSHOT_KEY)).willThrow(new RedisConnectionFailureException("down"));

            assertThatCode(() -> registry.resync()).doesNotThrowAnyException();

            assertThat(registry.getPhase("005930")).isEqualTo(BffServerMarketPhase.OPEN);
            assertThat(registry.getGlobalPhase()).isEqualTo(BffServerMarketPhase.OPEN);
        }

        @Test
        @DisplayName("[현재 동작] 스냅샷에서 사라진 종목은 캐시에서 지우지 않고 마지막 상태를 유지한다")
        void removedStock_keepsLastPhase() {
            given(hashOps.entries(SNAPSHOT_KEY)).willReturn(Map.of("005930", "OPEN"));
            registry.resync();
            given(hashOps.entries(SNAPSHOT_KEY)).willReturn(Map.of());

            registry.resync();

            assertThat(registry.getPhase("005930")).isEqualTo(BffServerMarketPhase.OPEN);
        }
    }
}

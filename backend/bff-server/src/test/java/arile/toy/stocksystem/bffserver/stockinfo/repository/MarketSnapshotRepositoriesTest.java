package arile.toy.stocksystem.bffserver.stockinfo.repository;

import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MarketSnapshotRepositoriesTest {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
    }

    /** save로 저장된 JSON을 꺼내 같은 키의 get 결과로 돌려주도록 설정 (저장 → 조회 왕복) */
    private String capturedJson(String key) {
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq(key), json.capture(), eq(TTL));
        given(valueOps.get(key)).willReturn(json.getValue());
        return json.getValue();
    }

    @Nested
    @DisplayName("국내 지수 스냅샷")
    class MarketMain {

        private MarketMainSnapshotRepository repository;

        @BeforeEach
        void setUp() {
            repository = new MarketMainSnapshotRepository(redisTemplate, OBJECT_MAPPER);
        }

        @Test
        @DisplayName("market:main:snapshot 키에 30초간 저장하고, 같은 모양으로 다시 읽는다")
        void saveAndGet() {
            MarketMainResponse response = new MarketMainResponse(null, null, null);

            repository.save(response);
            capturedJson("market:main:snapshot");

            assertThat(repository.getLatest()).isEqualTo(response);
        }

        @Test
        @DisplayName("스냅샷이 없거나, 깨졌거나, Redis 조회가 실패하면 null을 반환한다")
        void missingOrFailure_null() {
            assertThat(repository.getLatest()).isNull();

            given(valueOps.get("market:main:snapshot")).willReturn("{broken");
            assertThat(repository.getLatest()).isNull();

            given(valueOps.get("market:main:snapshot")).willThrow(new RedisConnectionFailureException("down"));
            assertThat(repository.getLatest()).isNull();
        }

        @Test
        @DisplayName("저장이 실패해도 예외를 던지지 않는다")
        void saveFails() {
            willThrow(new RedisConnectionFailureException("down"))
                    .given(valueOps).set(anyString(), anyString(), eq(TTL));

            assertThatCode(() -> repository.save(new MarketMainResponse(null, null, null)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("환율 스냅샷")
    class GlobalMarket {

        private GlobalMarketSnapshotRepository repository;

        @BeforeEach
        void setUp() {
            repository = new GlobalMarketSnapshotRepository(redisTemplate, OBJECT_MAPPER);
        }

        @Test
        @DisplayName("market:global:snapshot 키에 30초간 저장하고, 같은 모양으로 다시 읽는다")
        void saveAndGet() {
            GlobalMarketResponse response = new GlobalMarketResponse(List.of());

            repository.save(response);
            capturedJson("market:global:snapshot");

            assertThat(repository.getLatest()).isEqualTo(response);
        }

        @Test
        @DisplayName("스냅샷이 없거나 Redis 조회가 실패하면 null을 반환한다")
        void missingOrFailure_null() {
            assertThat(repository.getLatest()).isNull();

            given(valueOps.get("market:global:snapshot")).willThrow(new RedisConnectionFailureException("down"));
            assertThat(repository.getLatest()).isNull();
        }
    }

    @Nested
    @DisplayName("종목 상세 스냅샷")
    class StockDetail {

        private StockDetailSnapshotRepository repository;

        @BeforeEach
        void setUp() {
            repository = new StockDetailSnapshotRepository(redisTemplate, OBJECT_MAPPER);
        }

        @Test
        @DisplayName("stock:detail:snapshot:{종목} 키에 30초간 저장하고, 같은 종목으로 다시 읽는다")
        void saveAndGet() {
            StockDetailTickMessage message = mock(StockDetailTickMessage.class);
            given(message.stockCode()).willReturn("005930");
            given(valueOps.get("stock:detail:snapshot:005930")).willReturn("{\"stockCode\": \"005930\"}");

            repository.save(message);

            verify(valueOps).set(eq("stock:detail:snapshot:005930"), anyString(), eq(TTL));
            assertThat(repository.getLatest("005930").stockCode()).isEqualTo("005930");
        }

        @Test
        @DisplayName("스냅샷이 없거나 Redis 조회가 실패하면 null을 반환한다")
        void missingOrFailure_null() {
            assertThat(repository.getLatest("005930")).isNull();

            given(valueOps.get("stock:detail:snapshot:005930")).willThrow(new RedisConnectionFailureException("down"));
            assertThat(repository.getLatest("005930")).isNull();
        }
    }
}

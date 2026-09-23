package arile.toy.stocksystem.accountserver.leverage.repository;

import arile.toy.stocksystem.accountserver.leverage.dto.LeveragePositionInfo;
import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LeverageAccountRedisRepositoryTest {

    private static final String USERNAME = "user1";
    private static final String KEY = "account:leverage:" + USERNAME;

    private StringRedisTemplate redisTemplate;
    private HashOperations<String, Object, Object> hashOps;
    private ObjectMapper objectMapper;

    private LeverageAccountRedisRepository repository;

    private final Map<String, LeveragePositionInfo> positions = Map.of(
            "005930:X2", LeveragePositionInfo.of(10, 6, 700_000L, 700_100L, 350_000L, "MARGIN_CALL"));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        hashOps = mock(HashOperations.class);
        doReturn(hashOps).when(redisTemplate).opsForHash();

        objectMapper = new ObjectMapper();
        repository = new LeverageAccountRedisRepository(redisTemplate, objectMapper);
    }

    // ===== getPositions =====

    @Test
    @DisplayName("getPositions: positions 필드의 JSON을 포지션 맵으로 변환한다")
    void getPositions() throws Exception {
        given(hashOps.get(KEY, "positions")).willReturn(objectMapper.writeValueAsString(positions));

        assertThat(repository.getPositions(USERNAME)).isEqualTo(positions);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "not-json"})
    @DisplayName("getPositions: 값이 없거나 공백·깨진 JSON이면 수정 가능한 빈 맵을 반환한다")
    void getPositions_whenAbsentOrInvalid_returnsMutableEmptyMap(String json) {
        given(hashOps.get(KEY, "positions")).willReturn(json);

        Map<String, LeveragePositionInfo> result = repository.getPositions(USERNAME);

        assertThat(result).isEmpty();
        // LeveragePositionRedisSyncer가 반환된 맵에 put/remove 하므로 수정 가능해야 한다
        result.put("000660:X1_5", LeveragePositionInfo.of(1, 1, 1L, 1L, 1L, "NORMAL"));
        assertThat(result).hasSize(1);
    }

    // ===== savePositions =====

    @Test
    @DisplayName("savePositions: 포지션 맵을 JSON으로 직렬화해 positions 필드에 저장한다")
    void savePositions() throws Exception {
        repository.savePositions(USERNAME, positions);

        verify(hashOps).put(KEY, "positions", objectMapper.writeValueAsString(positions));
    }

    @Test
    @DisplayName("savePositions: 직렬화에 실패하면 저장하지 않고 예외를 전파하지 않는다")
    void savePositions_whenSerializationFails_doesNotSave() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        given(failingMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("fail") {});
        var failingRepository = new LeverageAccountRedisRepository(redisTemplate, failingMapper);

        failingRepository.savePositions(USERNAME, positions);

        verifyNoInteractions(hashOps);
    }

    // ===== marginStatus =====

    @Test
    @DisplayName("saveMarginStatus: marginStatus 필드에 저장한다")
    void saveMarginStatus() {
        repository.saveMarginStatus(USERNAME, "LIQUIDATION_PENDING");

        verify(hashOps).put(KEY, "marginStatus", "LIQUIDATION_PENDING");
    }

    @Test
    @DisplayName("getMarginStatus: 저장된 값을 반환하고, 없으면 NORMAL을 반환한다")
    void getMarginStatus() {
        given(hashOps.get(KEY, "marginStatus")).willReturn("MARGIN_CALL", (Object) null);

        assertThat(repository.getMarginStatus(USERNAME)).isEqualTo("MARGIN_CALL");
        assertThat(repository.getMarginStatus(USERNAME)).isEqualTo("NORMAL");
    }

    // ===== positionKey =====

    @Test
    @DisplayName("positionKey: 종목코드:배율 형식으로 만든다")
    void positionKey() {
        assertThat(LeverageAccountRedisRepository.positionKey("005930", LeverageRatio.X2)).isEqualTo("005930:X2");
        assertThat(LeverageAccountRedisRepository.positionKey("000660", LeverageRatio.X1_5)).isEqualTo("000660:X1_5");
    }
}

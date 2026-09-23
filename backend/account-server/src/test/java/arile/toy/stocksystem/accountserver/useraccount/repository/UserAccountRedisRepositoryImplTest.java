package arile.toy.stocksystem.accountserver.useraccount.repository;

import arile.toy.stocksystem.accountserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.accountserver.useraccount.dto.UserAccountMessage;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserAccountRedisRepositoryImplTest {

    private static final String USERNAME = "user1";
    private static final String KEY = "account:" + USERNAME;

    private StringRedisTemplate redisTemplate;
    private HashOperations<String, Object, Object> hashOps;
    private ObjectMapper objectMapper;

    private UserAccountRedisRepositoryImpl repository;

    private final Map<String, StockInfo> stocks =
            Map.of("005930", StockInfo.of(10, 8, 700000L, 705000L));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        hashOps = mock(HashOperations.class);
        doReturn(hashOps).when(redisTemplate).opsForHash();

        objectMapper = new ObjectMapper();
        repository = new UserAccountRedisRepositoryImpl(redisTemplate, objectMapper);
    }

    private Map<Object, Object> redisHash(Object availableCash, Object reservedCash,
                                          Object stocksJson, Object accountStatus) {
        Map<Object, Object> map = new HashMap<>();
        if (availableCash != null) map.put("availableCash", availableCash);
        if (reservedCash != null) map.put("reservedCash", reservedCash);
        if (stocksJson != null) map.put("stocks", stocksJson);
        if (accountStatus != null) map.put("accountStatus", accountStatus);
        return map;
    }

    // ===== save / saveStocks =====

    @Test
    @DisplayName("save: 계좌 정보를 hash로 저장하고 stocks는 JSON으로 직렬화한다")
    void save() throws Exception {
        UserAccountMessage account =
                new UserAccountMessage(USERNAME, 1000L, 200L, stocks, "NEGATIVE");

        repository.save(USERNAME, account);

        verify(hashOps).putAll(KEY, Map.of(
                "availableCash", "1000",
                "reservedCash", "200",
                "stocks", objectMapper.writeValueAsString(stocks),
                "accountStatus", "NEGATIVE"
        ));
    }

    @Test
    @DisplayName("save: stocks가 null이면 빈 JSON 객체로 저장한다")
    void save_withNullStocks() {
        UserAccountMessage account = UserAccountMessage.of(USERNAME, 1000L, 0L, null);

        repository.save(USERNAME, account);

        verify(hashOps).putAll(KEY, Map.of(
                "availableCash", "1000",
                "reservedCash", "0",
                "stocks", "{}",
                "accountStatus", "NORMAL"
        ));
    }

    @Test
    @DisplayName("saveStocks: stocks 필드만 JSON으로 저장한다")
    void saveStocks() throws Exception {
        repository.saveStocks(USERNAME, stocks);

        verify(hashOps).put(KEY, "stocks", objectMapper.writeValueAsString(stocks));
    }

    @Test
    @DisplayName("saveStocks: 직렬화에 실패하면 빈 JSON 객체로 저장한다")
    void saveStocks_whenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        given(failingMapper.writeValueAsString(any()))
                .willThrow(new JsonProcessingException("fail") {});
        var failingRepository = new UserAccountRedisRepositoryImpl(redisTemplate, failingMapper);

        failingRepository.saveStocks(USERNAME, stocks);

        verify(hashOps).put(KEY, "stocks", "{}");
    }

    // ===== findByUsername =====

    @Test
    @DisplayName("findByUsername: hash 값을 UserAccountMessage로 변환한다")
    void findByUsername() throws Exception {
        given(hashOps.entries(KEY)).willReturn(redisHash(
                "1000", "200", objectMapper.writeValueAsString(stocks), "NEGATIVE"));

        UserAccountMessage result = repository.findByUsername(USERNAME);

        assertThat(result).isEqualTo(
                new UserAccountMessage(USERNAME, 1000L, 200L, stocks, "NEGATIVE"));
    }

    @Test
    @DisplayName("findByUsername: Redis 결과가 null이면 null을 반환한다")
    void findByUsername_whenNull() {
        given(hashOps.entries(KEY)).willReturn(null);

        assertThat(repository.findByUsername(USERNAME)).isNull();
    }

    @Test
    @DisplayName("findByUsername: Redis 결과가 비어 있으면 null을 반환한다")
    void findByUsername_whenEmpty() {
        given(hashOps.entries(KEY)).willReturn(Map.of());

        assertThat(repository.findByUsername(USERNAME)).isNull();
    }

    @Test
    @DisplayName("findByUsername: 필드가 없으면 금액 0, 빈 stocks, NORMAL 상태로 채운다")
    void findByUsername_withMissingFields() {
        given(hashOps.entries(KEY)).willReturn(Map.of("unknown", "x"));

        UserAccountMessage result = repository.findByUsername(USERNAME);

        assertThat(result.availableCash()).isZero();
        assertThat(result.reservedCash()).isZero();
        assertThat(result.stocks()).isEmpty();
        assertThat(result.accountStatus()).isEqualTo("NORMAL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-json"})
    @DisplayName("findByUsername: stocks JSON이 공백이거나 깨져 있으면 빈 맵을 반환한다")
    void findByUsername_withInvalidStocksJson(String stocksJson) {
        given(hashOps.entries(KEY)).willReturn(redisHash("1000", "0", stocksJson, "NORMAL"));

        UserAccountMessage result = repository.findByUsername(USERNAME);

        assertThat(result.stocks()).isEmpty();
        assertThat(result.availableCash()).isEqualTo(1000L);
    }

    // ===== getAvailableCash / getReservedCash / getStocks =====

    @Test
    @DisplayName("계좌가 있으면 availableCash, reservedCash, stocks를 반환한다")
    void getters_whenAccountExists() throws Exception {
        given(hashOps.entries(KEY)).willReturn(redisHash(
                "1000", "200", objectMapper.writeValueAsString(stocks), "NORMAL"));

        assertThat(repository.getAvailableCash(USERNAME)).isEqualTo(1000L);
        assertThat(repository.getReservedCash(USERNAME)).isEqualTo(200L);
        assertThat(repository.getStocks(USERNAME)).isEqualTo(stocks);
    }

    @Test
    @DisplayName("계좌가 없으면 금액은 0, stocks는 빈 맵을 반환한다")
    void getters_whenAccountNotExists() {
        given(hashOps.entries(KEY)).willReturn(Map.of());

        assertThat(repository.getAvailableCash(USERNAME)).isZero();
        assertThat(repository.getReservedCash(USERNAME)).isZero();
        assertThat(repository.getStocks(USERNAME)).isEmpty();
    }

    // ===== updateAccountAfterClose / accountStatus =====

    @Test
    @DisplayName("updateAccountAfterClose: availableCash를 갱신하고 reservedCash를 0으로 초기화한다")
    void updateAccountAfterClose() {
        repository.updateAccountAfterClose(USERNAME, 5000L);

        verify(hashOps).putAll(KEY, Map.of("availableCash", "5000", "reservedCash", "0"));
    }

    @Test
    @DisplayName("saveAccountStatus: accountStatus 필드를 저장한다")
    void saveAccountStatus() {
        repository.saveAccountStatus(USERNAME, "SUSPENDED");

        verify(hashOps).put(KEY, "accountStatus", "SUSPENDED");
    }

    @Test
    @DisplayName("getAccountStatus: 저장된 상태를 반환한다")
    void getAccountStatus() {
        given(hashOps.get(KEY, "accountStatus")).willReturn("NEGATIVE");

        assertThat(repository.getAccountStatus(USERNAME)).isEqualTo("NEGATIVE");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("getAccountStatus: 값이 없으면 NORMAL을 반환한다")
    void getAccountStatus_whenAbsent(Object value) {
        given(hashOps.get(KEY, "accountStatus")).willReturn(value);

        assertThat(repository.getAccountStatus(USERNAME)).isEqualTo("NORMAL");
    }
}

package arile.toy.stocksystem.bffserver.account.service;

import arile.toy.stocksystem.bffserver.account.dto.AccountSnapshot;
import arile.toy.stocksystem.bffserver.account.dto.StockInfo;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPullServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AccountPullService accountPullService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("유효한 계좌 정보가 있을 때 AccountSnapshot 반환")
    void givenValidAccount_whenGetAccountMessage_thenReturnSnapshot() throws Exception {
        // given
        Map<Object, Object> redisData = new HashMap<>();
        redisData.put("availableCash", "100000");
        redisData.put("reservedCash", "50000");
        redisData.put("stocks", "{\"005930\":{\"quantity\":10,\"amount\":500000}}");

        when(hashOperations.entries("account:user1")).thenReturn(redisData);

        Map<String, StockInfo> stockMap = new HashMap<>();
        stockMap.put("005930", new StockInfo(10, 10, 50000));

        when(objectMapper.readValue(
                anyString(),
                ArgumentMatchers.<TypeReference<Map<String, StockInfo>>>any()
        )).thenReturn(stockMap);

        // when
        AccountSnapshot snapshot = accountPullService.getAccountMessage("user1");

        // then
        assertEquals(100000L, snapshot.availableCash());
        assertEquals(50000L, snapshot.reservedCash());
        assertEquals(1, snapshot.stocks().size());
    }

    @Test
    @DisplayName("주식 정보가 없을 때 빈 stocks 반환")
    void givenNoStocks_whenGetAccountMessage_thenReturnEmptyStocks() {
        // given
        Map<Object, Object> redisData = new HashMap<>();
        redisData.put("availableCash", "100000");
        redisData.put("reservedCash", "50000");

        when(hashOperations.entries("account:user1")).thenReturn(redisData);

        // when
        AccountSnapshot snapshot = accountPullService.getAccountMessage("user1");

        // then
        assertEquals(100000L, snapshot.availableCash());
        assertEquals(50000L, snapshot.reservedCash());
        assertTrue(snapshot.stocks().isEmpty());
    }

    @Test
    @DisplayName("주식 JSON 파싱 실패 시 무시하고 빈 stocks 반환")
    void givenInvalidStocksJson_whenParseFails_thenIgnoreAndReturnEmpty() throws Exception {
        // given
        Map<Object, Object> redisData = new HashMap<>();
        redisData.put("availableCash", "100000");
        redisData.put("reservedCash", "50000");
        redisData.put("stocks", "invalid-json");

        when(hashOperations.entries("account:user1")).thenReturn(redisData);
        when(objectMapper.readValue(
                anyString(),
                ArgumentMatchers.<TypeReference<Map<String, StockInfo>>>any()
        )).thenThrow(JsonProcessingException.class);

        // when
        AccountSnapshot snapshot = accountPullService.getAccountMessage("user1");

        // then
        assertTrue(snapshot.stocks().isEmpty());
    }

    @Test
    @DisplayName("Redis에 계좌 정보가 없으면 예외 발생")
    void givenEmptyRedisHash_whenGetAccountMessage_thenThrowException() {
        // given
        when(hashOperations.entries("account:user1")).thenReturn(Collections.emptyMap());

        // when & then
        assertThrows(
                RedisAccountNotFoundException.class,
                () -> accountPullService.getAccountMessage("user1")
        );
    }

    @Test
    @DisplayName("필수 필드가 없으면 기본값이 아닌 예외 발생")
    void givenMissingFields_whenGetAccountMessage_thenDefaultZero() {
        // given
        Map<Object, Object> redisData = new HashMap<>();
        when(hashOperations.entries("account:user1")).thenReturn(redisData);

        // when & then
        assertThrows(
                RedisAccountNotFoundException.class,
                () -> accountPullService.getAccountMessage("user1")
        );
    }
}

package arile.toy.stocksystem.stockserver.useraccount.repository;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.stockserver.useraccount.dto.StockServerAccountMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServerRedisAccountRepositoryTest {

    @Mock
    private RedisTemplate<String, StockServerAccountMessage> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private StockServerRedisAccountRepository repository;

    private final String username = "user1";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    @DisplayName("계좌 정보를 저장하면 Redis putAll이 호출된다")
    void givenAccount_whenSave_thenPutAllCalled() {
        // given
        StockServerAccountMessage account = new StockServerAccountMessage(
                username, 1000L, 500L,
                Map.of("005930", new StockInfo(10, 10, 50000))
        );

        // when
        repository.save(username, account);

        // then
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass((Class) Map.class);
        verify(hashOperations).putAll(eq("account:" + username), mapCaptor.capture());

        Map<String, Object> savedMap = mapCaptor.getValue();
        assertEquals(1000L, savedMap.get("availableCash"));
        assertEquals(500L, savedMap.get("reservedCash"));
        assertEquals(account.stocks(), savedMap.get("stocks"));
    }

    @Test
    @DisplayName("주식 정보를 저장하면 Redis put이 호출된다")
    void givenStocks_whenSaveStocks_thenPutCalledWithJson() throws JsonProcessingException {
        // given
        Map<String, StockInfo> stocks = Map.of("005930", new StockInfo(50, 50, 50000));
        String stocksJson = "{\"005930\":{\"stockCode\":\"005930\",\"quantity\":10}}";

        when(objectMapper.writeValueAsString(stocks)).thenReturn(stocksJson);

        // when
        repository.saveStocks(username, stocks);

        // then
        verify(hashOperations).put("account:" + username, "stocks", stocksJson);
    }

    @Test
    @DisplayName("저장된 계좌가 존재하면 username으로 조회 시 반환된다")
    void givenExistingEntry_whenFindByUsername_thenReturnAccount() {
        // given
        Map<Object, Object> stored = new HashMap<>();
        stored.put("availableCash", 1000L);
        stored.put("reservedCash", 500L);
        stored.put("stocks", Map.of("005930", new StockInfo(50, 50, 50000)));

        when(hashOperations.entries("account:" + username)).thenReturn(stored);

        // when
        StockServerAccountMessage result = repository.findByUsername(username);

        // then
        assertNotNull(result);
        assertEquals(1000L, result.availableCash());
        assertEquals(500L, result.reservedCash());
        assertEquals(1, result.stocks().size());
        assertEquals(50, result.stocks().get("005930").quantity());
    }

    @Test
    @DisplayName("저장된 계좌가 없으면 username으로 조회 시 null 반환")
    void givenNoEntry_whenFindByUsername_thenReturnNull() {
        // given
        when(hashOperations.entries("account:" + username)).thenReturn(Collections.emptyMap());

        // when
        StockServerAccountMessage result = repository.findByUsername(username);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("사용 가능한 현금을 조회하면 올바른 값 반환")
    void givenAvailableCash_whenGetAvailableCash_thenReturnCorrectValue() {
        // given
        Map<Object, Object> stored =
                Map.of("availableCash", 1000L, "reservedCash", 500L, "stocks", Map.of());
        when(hashOperations.entries("account:" + username)).thenReturn(stored);

        // when
        Long cash = repository.getAvailableCash(username);

        // then
        assertEquals(1000L, cash);
    }

    @Test
    @DisplayName("예약된 현금을 조회하면 올바른 값 반환")
    void givenReservedCash_whenGetReservedCash_thenReturnCorrectValue() {
        // given
        Map<Object, Object> stored =
                Map.of("availableCash", 1000L, "reservedCash", 500L, "stocks", Map.of());
        when(hashOperations.entries("account:" + username)).thenReturn(stored);

        // when
        Long reserved = repository.getReservedCash(username);

        // then
        assertEquals(500L, reserved);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    @DisplayName("장 마감 후 계좌를 갱신하면 Redis putAll이 호출된다")
    void givenUpdateAfterClose_whenUpdateAccountAfterClose_thenPutAllCalled() {
        // when
        repository.updateAccountAfterClose(username, 2000L);

        // then
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass((Class) Map.class);
        verify(hashOperations).putAll(eq("account:" + username), mapCaptor.capture());

        Map<String, Object> map = mapCaptor.getValue();
        assertEquals(2000L, map.get("availableCash"));
        assertEquals(0L, map.get("reservedCash"));
    }
}

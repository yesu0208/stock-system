package arile.toy.stocksystem.stockserver.userstock.service;

import arile.toy.stocksystem.stockserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.StockServerAccountRepository;
import arile.toy.stocksystem.stockserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.stockserver.userstock.repository.UserStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStockServiceTest {

    @Mock
    private UserStockRepository userStockRepository;

    @Mock
    private StockServerAccountRepository stockServerAccountRepository;

    @Mock
    private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private UserStockService userStockService;

    private final String username = "testUser";

    @Test
    @DisplayName("사용자가 보유한 주식을 정산하면 Redis에 저장하고 AccountUpdate 이벤트를 발행한다")
    void givenMatchingStocks_whenSettleStocks_thenSaveAndPublish() {
        // Given
        UserStockEntity entity = mock(UserStockEntity.class);

        when(entity.getStockCode()).thenReturn("005930");
        when(entity.getQuantity()).thenReturn(10);
        when(entity.getAmount()).thenReturn(100_000L);

        when(userStockRepository.findByUsername(username))
                .thenReturn(List.of(entity));

        Map<String, StockInfo> redisStocks = Map.of(
                "005930", StockInfo.of(10, 10000, 10)
        );

        when(stockServerAccountRepository.getStocks(username))
                .thenReturn(redisStocks);

        // When
        userStockService.settleStocks(Set.of(username));

        // Then
        verify(stockServerAccountRepository)
                .saveStocks(eq(username), anyMap());

        verify(accountUpdateEventPublisher)
                .publish(username);
    }

    @Test
    @DisplayName("Redis에 주식 정보가 null이어도 안전하게 처리하고 저장 및 이벤트 발행")
    void givenRedisStocksNull_whenSettleStocks_thenHandleSafely() {
        // Given
        when(userStockRepository.findByUsername(username))
                .thenReturn(Collections.emptyList());

        when(stockServerAccountRepository.getStocks(username))
                .thenReturn(null);

        // When
        userStockService.settleStocks(Set.of(username));

        // Then
        verify(stockServerAccountRepository)
                .saveStocks(eq(username), anyMap());

        verify(accountUpdateEventPublisher)
                .publish(username);
    }

    @Test
    @DisplayName("Redis에 존재하지 않는 추가 주식이 있어도 저장 및 이벤트 발행")
    void givenRedisHasExtraStock_whenSettleStocks_thenStillSaveAndPublish() {
        // Given
        UserStockEntity entity = mock(UserStockEntity.class);

        when(entity.getStockCode()).thenReturn("005930");
        when(entity.getQuantity()).thenReturn(5);
        when(entity.getAmount()).thenReturn(50_000L);

        when(userStockRepository.findByUsername(username))
                .thenReturn(List.of(entity));

        Map<String, StockInfo> redisStocks = Map.of(
                "000660", StockInfo.of(3, 80000, 3)
        );

        when(stockServerAccountRepository.getStocks(username))
                .thenReturn(redisStocks);

        // When
        userStockService.settleStocks(Set.of(username));

        // Then
        verify(stockServerAccountRepository)
                .saveStocks(eq(username), anyMap());

        verify(accountUpdateEventPublisher)
                .publish(username);
    }

    @Test
    @DisplayName("여러 사용자의 주식을 정산하면 각각 처리하고 저장 및 이벤트 발행")
    void givenMultipleUsers_whenSettleStocks_thenProcessEach() {
        // Given
        String user2 = "user2";

        when(userStockRepository.findByUsername(anyString()))
                .thenReturn(Collections.emptyList());

        when(stockServerAccountRepository.getStocks(anyString()))
                .thenReturn(new HashMap<>());

        // When
        userStockService.settleStocks(Set.of(username, user2));

        // Then
        verify(stockServerAccountRepository, times(2))
                .saveStocks(anyString(), anyMap());

        verify(accountUpdateEventPublisher, times(2))
                .publish(anyString());
    }
}

package arile.toy.stocksystem.stockserver.userstock.repository;

import arile.toy.stocksystem.stockserver.userstock.entity.UserStockEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserStockRepositoryTest {

    @Spy
    private UserStockRepository userStockRepository;

    @Test
    @DisplayName("저장된 주식을 조회하면 해당 주식을 반환한다")
    void givenSavedStock_whenFindByUsernameAndStockCode_thenReturnStock() {
        // given
        UserStockEntity stock = new UserStockEntity();
        stock.setUsername("user1");
        stock.setStockCode("005930");

        when(userStockRepository.findByUsernameAndStockCode("user1", "005930"))
                .thenReturn(Optional.of(stock));

        // when
        Optional<UserStockEntity> found = userStockRepository.findByUsernameAndStockCode("user1", "005930");

        // then
        assertTrue(found.isPresent());
        assertEquals(stock, found.get());
        verify(userStockRepository).findByUsernameAndStockCode("user1", "005930");
    }

    @Test
    @DisplayName("주식이 없으면 조회 시 빈 Optional을 반환한다")
    void givenNoStock_whenFindByUsernameAndStockCode_thenReturnEmpty() {
        // given
        when(userStockRepository.findByUsernameAndStockCode("user1", "000000"))
                .thenReturn(Optional.empty());

        // when
        Optional<UserStockEntity> found = userStockRepository.findByUsernameAndStockCode("user1", "000000");

        // then
        assertFalse(found.isPresent());
        verify(userStockRepository).findByUsernameAndStockCode("user1", "000000");
    }

    @Test
    @DisplayName("여러 주식이 있을 경우 사용자별 조회 시 모든 주식을 반환한다")
    void givenMultipleStocks_whenFindByUsername_thenReturnList() {
        // given
        UserStockEntity stock1 = new UserStockEntity();
        stock1.setUsername("user1");
        stock1.setStockCode("005930");

        UserStockEntity stock2 = new UserStockEntity();
        stock2.setUsername("user1");
        stock2.setStockCode("000660");

        List<UserStockEntity> stocks = List.of(stock1, stock2);

        when(userStockRepository.findByUsername("user1")).thenReturn(stocks);

        // when
        List<UserStockEntity> found = userStockRepository.findByUsername("user1");

        // then
        assertEquals(2, found.size());
        assertTrue(found.contains(stock1));
        assertTrue(found.contains(stock2));
        verify(userStockRepository).findByUsername("user1");
    }

    @Test
    @DisplayName("사용자가 보유한 주식이 없으면 빈 리스트를 반환한다")
    void givenNoStocks_whenFindByUsername_thenReturnEmptyList() {
        // given
        when(userStockRepository.findByUsername("user2")).thenReturn(Collections.emptyList());

        // when
        List<UserStockEntity> found = userStockRepository.findByUsername("user2");

        // then
        assertTrue(found.isEmpty());
        verify(userStockRepository).findByUsername("user2");
    }
}

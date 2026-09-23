package arile.toy.stocksystem.accountserver.userstock.service;

import arile.toy.stocksystem.accountserver.useraccount.dto.StockInfo;
import arile.toy.stocksystem.accountserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRedisRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import arile.toy.stocksystem.accountserver.userstock.entity.UserStockEntity;
import arile.toy.stocksystem.accountserver.userstock.repository.UserStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserStockServiceTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";

    @Mock
    private UserStockRepository userStockRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserAccountRedisRepository userAccountRedisRepository;

    @Mock
    private AccountUpdateEventPublisher accountUpdateEventPublisher;

    @InjectMocks
    private UserStockService userStockService;

    /** DB: 10주, 매입금액 700,000, 매입원금 705,000 */
    private static UserStockEntity dbStock() {
        return UserStockEntity.of(USERNAME, STOCK_CODE, 700_000L, 705_000L, 10);
    }

    /** DB 기준으로 재구성된 Redis 종목 정보 (availableQuantity = quantity) */
    private static final Map<String, StockInfo> EXPECTED =
            Map.of(STOCK_CODE, StockInfo.of(10, 10, 700_000L, 705_000L));

    private void verifySavedAndPublished(Map<String, StockInfo> expected) {
        verify(userAccountRedisRepository).saveStocks(USERNAME, expected);
        verify(accountUpdateEventPublisher).publish(USERNAME);
    }

    @Nested
    @DisplayName("settleStocks")
    class SettleStocks {

        static Stream<Arguments> redisStockCases() {
            return Stream.of(
                    Arguments.of("Redis에 종목 없음", Map.of()),
                    Arguments.of("수량 불일치",
                            Map.of(STOCK_CODE, StockInfo.of(9, 9, 700_000L, 705_000L))),
                    Arguments.of("매입금액 null",
                            Map.of(STOCK_CODE, new StockInfo(10, 10, null, 705_000L))),
                    Arguments.of("매입금액 불일치",
                            Map.of(STOCK_CODE, StockInfo.of(10, 10, 690_000L, 705_000L))),
                    Arguments.of("완전 일치 (예약 수량만 남아 있음)",
                            Map.of(STOCK_CODE, StockInfo.of(10, 7, 700_000L, 705_000L)))
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("redisStockCases")
        @DisplayName("Redis 상태와 무관하게 DB 기준으로 종목을 재구성하고 예약 수량을 해제한다")
        void rebuildsFromDb(String description, Map<String, StockInfo> redisStocks) {
            given(userStockRepository.findByUsername(USERNAME)).willReturn(List.of(dbStock()));
            given(userAccountRedisRepository.getStocks(USERNAME)).willReturn(redisStocks);

            userStockService.settleStocks(Set.of(USERNAME));

            verifySavedAndPublished(EXPECTED);
        }

        @Test
        @DisplayName("Redis stocks가 null이면 빈 맵으로 간주하고 DB 기준으로 저장한다")
        void whenRedisStocksNull_rebuildsFromDb() {
            given(userStockRepository.findByUsername(USERNAME)).willReturn(List.of(dbStock()));
            given(userAccountRedisRepository.getStocks(USERNAME)).willReturn(null);

            userStockService.settleStocks(Set.of(USERNAME));

            verifySavedAndPublished(EXPECTED);
        }

        @Test
        @DisplayName("Redis에만 있는 종목은 제거된다")
        void whenRedisHasExtraStock_removesIt() {
            given(userStockRepository.findByUsername(USERNAME)).willReturn(List.of(dbStock()));
            given(userAccountRedisRepository.getStocks(USERNAME)).willReturn(Map.of(
                    STOCK_CODE, StockInfo.of(10, 10, 700_000L, 705_000L),
                    "000660", StockInfo.of(3, 3, 300_000L, 301_000L)
            ));

            userStockService.settleStocks(Set.of(USERNAME));

            verifySavedAndPublished(EXPECTED);
        }

        @Test
        @DisplayName("DB에 보유 종목이 없으면 빈 종목으로 저장한다")
        void whenNoDbStocks_savesEmpty() {
            given(userStockRepository.findByUsername(USERNAME)).willReturn(List.of());
            given(userAccountRedisRepository.getStocks(USERNAME)).willReturn(Map.of());

            userStockService.settleStocks(Set.of(USERNAME));

            verifySavedAndPublished(Map.of());
        }
    }

    @Test
    @DisplayName("settleAllStocks: 전체 username을 중복 없이 정산한다")
    void settleAllStocks() {
        given(userAccountRepository.findAllUsernames()).willReturn(List.of(USERNAME, USERNAME));
        given(userStockRepository.findByUsername(USERNAME)).willReturn(List.of());
        given(userAccountRedisRepository.getStocks(USERNAME)).willReturn(Map.of());

        userStockService.settleAllStocks();

        verify(userStockRepository, times(1)).findByUsername(USERNAME);
        verify(userAccountRedisRepository).saveStocks(USERNAME, Map.of());
    }
}

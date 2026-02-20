package arile.toy.stocksystem.bffserver.session;

import arile.toy.stocksystem.bffserver.account.dto.AccountResponse;
import arile.toy.stocksystem.bffserver.account.service.AccountCalculator;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponseMessage;
import arile.toy.stocksystem.bffserver.autoorder.repository.BffServerAutoOrderResponseRepository;
import arile.toy.stocksystem.bffserver.exception.server.RedisAccountNotFoundException;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerBidAskPriceRepository;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerTradePriceRepository;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponseMessage;
import arile.toy.stocksystem.bffserver.order.repository.BffServerOrderResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialDataServiceTest {

    @Mock
    private AccountCalculator accountCalculator;

    @Mock
    private BffServerOrderResponseRepository bffServerOrderResponseRepository;

    @Mock
    private BffServerAutoOrderResponseRepository bffServerAutoOrderResponseRepository;

    @Mock
    private BffServerTradePriceRepository bffServerTradePriceRepository;

    @Mock
    private BffServerBidAskPriceRepository bffServerBidAskPriceRepository;

    @InjectMocks
    private InitialDataService initialDataService;

    @Test
    @DisplayName("정상 계좌 데이터 반환")
    void givenValidUsername_whenGetAccountData_thenReturnsAccount() {
        // given
        String username = "user1";
        AccountResponse mockAccount = mock(AccountResponse.class);
        when(accountCalculator.calculate(username)).thenReturn(mockAccount);

        // when
        Optional<AccountResponse> result = initialDataService.getAccountData(username);

        // then
        assertTrue(result.isPresent());
        assertEquals(mockAccount, result.get());
    }

    @Test
    @DisplayName("RedisAccountNotFoundException 발생 시 Optional.empty 반환")
    void givenUsername_whenRedisAccountNotFound_thenReturnsEmpty() {
        // given
        String username = "user2";
        when(accountCalculator.calculate(username)).thenThrow(new RedisAccountNotFoundException());

        // when
        Optional<AccountResponse> result = initialDataService.getAccountData(username);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("정상 주문 데이터 반환")
    void givenValidUsername_whenGetOrderData_thenReturnsOrderList() {
        // given
        String username = "user1";
        List<OrderResponseMessage> mockList = List.of(mock(OrderResponseMessage.class));
        when(bffServerOrderResponseRepository.findAll(username)).thenReturn(mockList);

        // when
        Optional<List<OrderResponseMessage>> result = initialDataService.getOrderData(username);

        // then
        assertTrue(result.isPresent());
        assertEquals(mockList, result.get());
    }

    @Test
    @DisplayName("정상 자동주문 데이터 반환")
    void givenValidUsername_whenGetAutoOrderData_thenReturnsAutoOrderList() {
        // given
        String username = "user1";
        List<AutoOrderResponseMessage> mockList = List.of(mock(AutoOrderResponseMessage.class));
        when(bffServerAutoOrderResponseRepository.findAll(username)).thenReturn(mockList);

        // when
        Optional<List<AutoOrderResponseMessage>> result = initialDataService.getAutoOrderData(username);

        // then
        assertTrue(result.isPresent());
        assertEquals(mockList, result.get());
    }

    @Test
    @DisplayName("정상 거래 가격 데이터 반환")
    void givenValidStockCode_whenGetTradePriceData_thenReturnsTradePriceMessage() {
        // given
        String stockCode = "005930";
        BffServerTradePriceTickMessage mockMessage = mock(BffServerTradePriceTickMessage.class);
        when(bffServerTradePriceRepository.findByStockCode(stockCode)).thenReturn(mockMessage);

        // when
        Optional<BffServerTradePriceTickMessage> result = initialDataService.getTradePriceData(stockCode);

        // then
        assertTrue(result.isPresent());
        assertEquals(mockMessage, result.get());
    }

    @Test
    @DisplayName("정상 호가 데이터 반환")
    void givenValidStockCode_whenGetBidAskPriceData_thenReturnsBidAskMessage() {
        // given
        String stockCode = "005930";
        BffServerBidAskPriceTickMessage mockMessage = mock(BffServerBidAskPriceTickMessage.class);
        when(bffServerBidAskPriceRepository.findByStockCode(stockCode)).thenReturn(mockMessage);

        // when
        Optional<BffServerBidAskPriceTickMessage> result = initialDataService.getBidAskPriceData(stockCode);

        // then
        assertTrue(result.isPresent());
        assertEquals(mockMessage, result.get());
    }

    @Test
    @DisplayName("각 메서드에서 예외 발생 시 Optional.empty 반환")
    void givenException_whenCallMethods_thenReturnsEmpty() {
        // given
        String username = "userX";
        String stockCode = "999999";

        when(accountCalculator.calculate(username)).thenThrow(new RuntimeException());
        when(bffServerOrderResponseRepository.findAll(username)).thenThrow(new RuntimeException());
        when(bffServerAutoOrderResponseRepository.findAll(username)).thenThrow(new RuntimeException());
        when(bffServerTradePriceRepository.findByStockCode(stockCode)).thenThrow(new RuntimeException());
        when(bffServerBidAskPriceRepository.findByStockCode(stockCode)).thenThrow(new RuntimeException());

        // when & then
        assertTrue(initialDataService.getAccountData(username).isEmpty());
        assertTrue(initialDataService.getOrderData(username).isEmpty());
        assertTrue(initialDataService.getAutoOrderData(username).isEmpty());
        assertTrue(initialDataService.getTradePriceData(stockCode).isEmpty());
        assertTrue(initialDataService.getBidAskPriceData(stockCode).isEmpty());
    }
}

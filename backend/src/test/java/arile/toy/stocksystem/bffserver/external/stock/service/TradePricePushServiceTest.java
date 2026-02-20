package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceClientTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisTradePriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradePricePushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BffServerRedisTradePriceRepository bffServerTradePriceRepository;

    @InjectMocks
    private TradePricePushService service;

    @Test
    @DisplayName("주어진 종목 코드로 TradePrice 메시지 푸시")
    void givenStockCode_whenPush_thenSendsTradePriceMessage() {
        // given
        String stockCode = "005930";
        var tickMessage = mock(BffServerTradePriceTickMessage.class);
        when(bffServerTradePriceRepository.findByStockCode(stockCode))
                .thenReturn(tickMessage);

        var clientMessage = mock(BffServerTradePriceClientTickMessage.class);

        try (MockedStatic<BffServerTradePriceClientTickMessage> mockedStatic =
                     mockStatic(BffServerTradePriceClientTickMessage.class)) {

            mockedStatic.when(() -> BffServerTradePriceClientTickMessage.fromTickMessage(tickMessage))
                    .thenReturn(clientMessage);

            // when
            service.push(stockCode);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/stock/" + stockCode),
                    eq(clientMessage)
            );

            verify(bffServerTradePriceRepository).findByStockCode(stockCode);
            verifyNoMoreInteractions(messagingTemplate);
        }
    }
}

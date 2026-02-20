package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.event.PriceLevel;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisBidAskPriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidAskPricePushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BffServerRedisBidAskPriceRepository bffServerRedisBidAskPriceRepository;

    @InjectMocks
    private BidAskPricePushService service;

    @Test
    @DisplayName("종목 코드로 BidAskPriceTick 메시지 푸시")
    void givenStockCode_whenPush_thenSendsBidAskMessage() {
        // given
        String stockCode = "005930";

        List<PriceLevel> bids = List.of();
        List<PriceLevel> asks = List.of();

        BffServerBidAskPriceTickMessage mockMessage = new BffServerBidAskPriceTickMessage(
                TickMessageType.BIDASKPRICE,
                stockCode,
                bids,
                asks,
                0,
                0
        );

        when(bffServerRedisBidAskPriceRepository.findByStockCode(stockCode))
                .thenReturn(mockMessage);

        // when
        service.push(stockCode);

        // then
        verify(messagingTemplate).convertAndSend(
                eq("/sub/stock/" + stockCode),
                eq(mockMessage)
        );

        verifyNoMoreInteractions(messagingTemplate);
        verify(bffServerRedisBidAskPriceRepository).findByStockCode(stockCode);
    }
}

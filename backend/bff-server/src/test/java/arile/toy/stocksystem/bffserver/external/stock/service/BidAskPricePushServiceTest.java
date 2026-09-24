package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisBidAskPriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BidAskPricePushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerRedisBidAskPriceRepository bffServerRedisBidAskPriceRepository;

    @InjectMocks
    private BidAskPricePushService service;

    @Test
    @DisplayName("저장된 호가를 그대로 종목 채널로 보낸다")
    void push() {
        BffServerBidAskPriceTickMessage bidAsk = mock(BffServerBidAskPriceTickMessage.class);
        given(bffServerRedisBidAskPriceRepository.findByStockCode("005930")).willReturn(bidAsk);

        service.push("005930");

        verify(messagingTemplate).convertAndSend("/sub/stock/005930", bidAsk);
    }

    @Test
    @DisplayName("호가가 없으면 보내지 않는다 (null 페이로드 전송 예외 방지)")
    void notFound_skipped() {
        given(bffServerRedisBidAskPriceRepository.findByStockCode("005930")).willReturn(null);

        service.push("005930");

        verifyNoInteractions(messagingTemplate);
    }
}

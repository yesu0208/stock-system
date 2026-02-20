package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisBidAskPriceEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.repository.StockServerRedisBidAskPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class BidAskPriceTickMessageHandlerTest {

    @Mock
    private RedisBidAskPriceEventPublisher redisBidAskPriceEventPublisher;

    @Mock
    private StockServerRedisBidAskPriceRepository stockServerBidAskPriceRepository;

    @InjectMocks
    private BidAskPriceTickMessageHandler handler;

    private String sampleMessage;

    @BeforeEach
    void setup() {
        sampleMessage = "0|H0STASP0|1|000660^151808^0^840000^841000^842000^843000^844000^845000^846000^847000^848000^849000^839000^838000^837000^836000^835000^834000^833000^832000^831000^830000^492^2060^5367^4809^5132^9266^3575^5270^14118^21748^6005^3569^3234^1187^2525^1589^1468^595^2170^4441^71837^26783^0^0^0^0^229089^-842000^5^-100.00^5331310^4^1^0^0^0^839500^0^0";
    }

    @Test
    @DisplayName("Bid/Ask 메시지를 처리하면 DB에 저장하고 Redis로 이벤트 발행")
    void givenBidAskMessage_whenHandle_thenSaveAndPublish() {
        // When
        handler.handle(sampleMessage);

        // Then
        verify(stockServerBidAskPriceRepository, times(1)).save(any());
        verify(redisBidAskPriceEventPublisher, times(1)).publish(any(BidAskPriceTickEvent.class));
    }
}
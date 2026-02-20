package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisStockSummaryEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.repository.StockServerRedisStockSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class StockSummaryTickMessageHandlerTest {

    @Mock
    private RedisStockSummaryEventPublisher redisStockSummaryEventPublisher;

    @Mock
    private StockServerRedisStockSummaryRepository stockServerStockSummaryRepository;

    @InjectMocks
    private StockSummaryTickMessageHandler handler;

    private String sampleMessage;

    @BeforeEach
    void setup() {
        sampleMessage = "0|H0STCNT0|1|000660^151807^840000^5^-2000^-0.24^819382.82^805000^850000^791000^840000^839000^1^5331286^4368365856500^83685^159421^75736^115.76^2364738^2737383^1^0.52^97.40^090019^2^35000^114153^5^-10000^090641^2^49000^20260206^20^N^482^6009^71857^26787^0.73^5051661^105.54^0^^805000";
    }

    @Test
    @DisplayName("Stock Summary Tick 메시지를 처리하면 저장 후 이벤트를 발행한다")
    void givenStockSummaryMessage_whenHandle_thenSaveAndPublish() {
        // when
        handler.handle(sampleMessage);

        // then
        verify(stockServerStockSummaryRepository, times(1)).save(any());
        verify(redisStockSummaryEventPublisher, times(1)).publish(any(StockSummaryTickEvent.class));
    }
}

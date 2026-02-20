package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisTradePriceEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.repository.StockServerRedisTradePriceRepository;
import arile.toy.stocksystem.stockserver.market.phase.MarketPhaseService;
import arile.toy.stocksystem.stockserver.autoorder.sevice.AutoOrderTriggerService;
import arile.toy.stocksystem.stockserver.trade.service.TradeMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradePriceTickMessageHandlerTest {

    @Mock
    private RedisTradePriceEventPublisher redisTradePriceEventPublisher;

    @Mock
    private StockServerRedisTradePriceRepository stockServerTradePriceRepository;

    @Mock
    private TradeMatchingService tradeMatchingService;

    @Mock
    private AutoOrderTriggerService autoOrderTriggerService;

    @Mock
    private MarketPhaseService marketPhaseService;

    @InjectMocks
    private TradePriceTickMessageHandler handler;

    private String sampleMessage;

    @BeforeEach
    void setup() {
        sampleMessage = "0|H0STCNT0|1|000660^151807^840000^5^-2000^-0.24^819382.82^805000^850000^791000^840000^839000^1^5331286^4368365856500^83685^159421^75736^115.76^2364738^2737383^1^0.52^97.40^090019^2^35000^114153^5^-10000^090641^2^49000^20260206^20^N^482^6009^71857^26787^0.73^5051661^105.54^0^^805000";
    }

    @Test
    @DisplayName("Trade Price Tick 메시지를 처리하면 저장, 이벤트 발행 및 트리거 서비스 호출이 수행된다")
    void givenTradePriceMessage_whenHandle_thenSavePublishAndTriggerServices() {
        // when
        handler.handle(sampleMessage);

        // then
        verify(stockServerTradePriceRepository, times(1)).save(any());
        verify(redisTradePriceEventPublisher, times(1)).publish(any(TradePriceTickEvent.class));
        verify(autoOrderTriggerService, times(1)).getExternalTickMessageAndTrigger(any());
        verify(tradeMatchingService, times(1)).getExternalTickMessageAndTrade(any());
        verify(marketPhaseService, times(1)).closeMarketAfterClosingCall(anyString(), anyString());
    }
}
package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceClientTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisTradePriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TradePricePushServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BffServerRedisTradePriceRepository bffServerTradePriceRepository;

    @InjectMocks
    private TradePricePushService service;

    @Test
    @DisplayName("체결가를 클라이언트용 메시지로 변환해 종목 채널로 보내며, 전일 종가는 현재가 - 전일 대비로 계산한다")
    void push() {
        given(bffServerTradePriceRepository.findByStockCode("005930")).willReturn(
                new BffServerTradePriceTickMessage(TickMessageType.TRADEPRICE, "005930", "090001",
                        71_000, 1_000, 70_000, "1.43", 70_000, 71_500, 69_800, 10, 1_000, 71_000_000L,
                        400, 600, "1", 900));

        service.push("005930");

        ArgumentCaptor<BffServerTradePriceClientTickMessage> captor =
                ArgumentCaptor.forClass(BffServerTradePriceClientTickMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/stock/005930"), captor.capture());

        BffServerTradePriceClientTickMessage sent = captor.getValue();
        assertThat(sent.tickMessageType()).isEqualTo(TickMessageType.TRADEPRICE);
        assertThat(sent.stockCode()).isEqualTo("005930");
        assertThat(sent.curPrice()).isEqualTo(71_000);
        assertThat(sent.prevCloseDiff()).isEqualTo(1_000);
        assertThat(sent.highPrice()).isEqualTo(71_500);
        assertThat(sent.totalTradingValue()).isEqualTo(71_000_000L);
        assertThat(sent.prevClosePrice()).isEqualTo(70_000);
    }

    @Test
    @DisplayName("체결가가 없으면 보내지 않는다")
    void notFound_skipped() {
        given(bffServerTradePriceRepository.findByStockCode("005930")).willReturn(null);

        service.push("005930");

        verifyNoInteractions(messagingTemplate);
    }
}

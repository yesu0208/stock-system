package arile.toy.stocksystem.bffserver.external.stock.service;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryClientTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.repository.BffServerRedisStockSummaryRepository;
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
class StockSummaryPushServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BffServerRedisStockSummaryRepository bffServerStockSummaryRepository;

    @InjectMocks
    private StockSummaryPushService service;

    @Test
    @DisplayName("Stock Summary 전체 메시지 푸시")
    void givenStockSummaries_whenPushAll_thenSendsClientMessages() {
        // given
        BffServerStockSummaryTickMessage tickMessage1 = mock(BffServerStockSummaryTickMessage.class);
        BffServerStockSummaryTickMessage tickMessage2 = mock(BffServerStockSummaryTickMessage.class);
        List<BffServerStockSummaryTickMessage> tickMessages = List.of(tickMessage1, tickMessage2);

        when(bffServerStockSummaryRepository.findAll()).thenReturn(tickMessages);

        BffServerStockSummaryClientTickMessage clientMessage1 = mock(BffServerStockSummaryClientTickMessage.class);
        BffServerStockSummaryClientTickMessage clientMessage2 = mock(BffServerStockSummaryClientTickMessage.class);

        try (var mockedStatic = mockStatic(BffServerStockSummaryClientTickMessage.class)) {
            mockedStatic.when(() -> BffServerStockSummaryClientTickMessage.fromBiffServerStockSummaryTickMessage(tickMessage1))
                    .thenReturn(clientMessage1);
            mockedStatic.when(() -> BffServerStockSummaryClientTickMessage.fromBiffServerStockSummaryTickMessage(tickMessage2))
                    .thenReturn(clientMessage2);

            // when
            service.pushAll();

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/stock/summary"),
                    eq(List.of(clientMessage1, clientMessage2))
            );
            verify(bffServerStockSummaryRepository).findAll();
            verifyNoMoreInteractions(messagingTemplate);
        }
    }
}

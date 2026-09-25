package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.external.stock.TickMessages;
import arile.toy.stocksystem.stockserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.PriceLevel;
import arile.toy.stocksystem.stockserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisBidAskPriceEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisStockSummaryEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.repository.StockServerRedisBidAskPriceRepository;
import arile.toy.stocksystem.stockserver.external.stock.repository.StockServerRedisStockSummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Handler] 호가·요약·상태 메시지 처리 테스트")
@ExtendWith(MockitoExtension.class)
class QuoteTickMessageHandlerTest {

    @Nested
    @DisplayName("호가")
    class BidAsk {

        @Mock private RedisBidAskPriceEventPublisher publisher;
        @Mock private StockServerRedisBidAskPriceRepository repository;

        @DisplayName("매도·매수 10호가와 잔량, 총잔량을 파싱해 저장·발행한다")
        @Test
        void givenTick_whenHandling_thenParsesLevels() {
            var sut = new BidAskPriceTickMessageHandler(publisher, repository);

            sut.handle(TickMessages.message("H0STASP0", TickMessages.bidAskRecord("005930")));

            ArgumentCaptor<BidAskPriceTickMessage> captor = ArgumentCaptor.forClass(BidAskPriceTickMessage.class);
            then(repository).should().save(captor.capture());
            BidAskPriceTickMessage message = captor.getValue();
            assertThat(message.asks()).hasSize(10).first().isEqualTo(new PriceLevel(70_000, 1));
            assertThat(message.bids()).hasSize(10).first().isEqualTo(new PriceLevel(71_000, 11));
            assertThat(message.totalAskNum()).isEqualTo(210);
            then(publisher).should().publish(new BidAskPriceTickEvent("005930"));
        }

        @DisplayName("파싱할 수 없는 레코드는 건너뛰고 다음 레코드를 처리한다")
        @Test
        void givenInvalidRecord_whenHandling_thenSkipsIt() {
            var sut = new BidAskPriceTickMessageHandler(publisher, repository);
            String[] invalid = TickMessages.bidAskRecord("005930");
            invalid[3] = "abc";

            sut.handle(TickMessages.message("H0STASP0", invalid, TickMessages.bidAskRecord("000660")));

            then(repository).should().save(argThat(m -> m.stockCode().equals("000660")));
            then(repository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("요약")
    class Summary {

        @Mock private RedisStockSummaryEventPublisher publisher;
        @Mock private StockServerRedisStockSummaryRepository repository;

        @DisplayName("현재가·전일 대비를 저장·발행하고, 파싱할 수 없는 레코드는 건너뛴다")
        @Test
        void givenTicks_whenHandling_thenSavesValid() {
            var sut = new StockSummaryTickMessageHandler(publisher, repository);

            sut.handle(TickMessages.message("H0STCNT0",
                    TickMessages.tradeRecord("005930", "70000", -300),
                    TickMessages.tradeRecord("000660", "x", 0)));

            then(repository).should().save(new StockSummaryTickMessage("005930", 70_000, -300));
            then(repository).shouldHaveNoMoreInteractions();
            then(publisher).should().publish(new StockSummaryTickEvent("005930"));
        }
    }

    @Nested
    @DisplayName("상태")
    class State {

        @DisplayName("구독 결과·PINGPONG·깨진 JSON 모두 예외 없이 처리한다")
        @Test
        void givenStateMessages_whenHandling_thenDoesNotThrow() {
            var sut = new StateTickMessageHandler(new ObjectMapper());

            assertThatNoException().isThrownBy(() -> {
                sut.handle("{\"body\":{\"msg1\":\"SUBSCRIBE SUCCESS\"}}");
                sut.handle("{\"body\":{\"msg1\":\"UNSUBSCRIBE SUCCESS\"}}");
                sut.handle("{\"body\":{\"msg1\":\"UNSUBSCRIBE ERROR(not found!)\"}}");
                sut.handle("{\"body\":{\"msg1\":\"ALREADY IN SUBSCRIBE\"}}");
                sut.handle("{\"body\":{\"msg1\":\"OTHER\"}}");
                sut.handle("{\"header\":{\"tr_id\":\"PINGPONG\",\"datetime\":\"20260925090000\"}}");
                sut.handle("{not json");
            });
        }
    }
}

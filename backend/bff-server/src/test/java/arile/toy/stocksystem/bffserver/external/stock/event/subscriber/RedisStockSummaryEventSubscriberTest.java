package arile.toy.stocksystem.bffserver.external.stock.event.subscriber;

import arile.toy.stocksystem.bffserver.external.stock.event.StockSummaryTickEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisStockSummaryEventSubscriberTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final RedisStockSummaryEventSubscriber subscriber =
            new RedisStockSummaryEventSubscriber(new ObjectMapper(), eventPublisher);

    private static Message message(String json) {
        return new DefaultMessage("summary:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("시세 요약 메시지를 StockSummaryTickEvent로 바꿔 스프링 이벤트로 발행한다")
    void onMessage() {
        subscriber.onMessage(message("{\"stockCode\": \"005930\"}"), null);

        verify(eventPublisher).publishEvent(new StockSummaryTickEvent("005930"));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 예외 없이 무시한다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(eventPublisher);
    }
}

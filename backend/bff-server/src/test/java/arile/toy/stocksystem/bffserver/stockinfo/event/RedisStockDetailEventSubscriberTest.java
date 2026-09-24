package arile.toy.stocksystem.bffserver.stockinfo.event;

import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.repository.StockDetailSnapshotRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisStockDetailEventSubscriberTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final StockDetailSnapshotRepository snapshotRepository = mock(StockDetailSnapshotRepository.class);
    private final RedisStockDetailEventSubscriber subscriber = new RedisStockDetailEventSubscriber(
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
            messagingTemplate, snapshotRepository);

    private static Message message(String json) {
        return new DefaultMessage("stock-detail:event".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("종목 상세를 종목별 채널로 먼저 전송하고, 이어서 스냅샷으로 저장한다")
    void onMessage() {
        subscriber.onMessage(message("{\"stockCode\": \"005930\"}"), null);

        ArgumentCaptor<StockDetailTickMessage> captor = ArgumentCaptor.forClass(StockDetailTickMessage.class);
        InOrder inOrder = inOrder(messagingTemplate, snapshotRepository);
        inOrder.verify(messagingTemplate).convertAndSend(eq("/sub/stock/005930"), captor.capture());
        inOrder.verify(snapshotRepository).save(captor.getValue());

        assertThat(captor.getValue().stockCode()).isEqualTo("005930");
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 전송도 저장도 하지 않는다")
    void brokenJson_ignored() {
        assertThatCode(() -> subscriber.onMessage(message("{broken"), null)).doesNotThrowAnyException();

        verifyNoInteractions(messagingTemplate, snapshotRepository);
    }
}

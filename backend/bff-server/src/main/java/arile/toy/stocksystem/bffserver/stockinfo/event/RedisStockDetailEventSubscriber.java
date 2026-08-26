package arile.toy.stocksystem.bffserver.stockinfo.event;

import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.repository.StockDetailSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStockDetailEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final StockDetailSnapshotRepository snapshotRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            StockDetailTickMessage tickMessage =
                    objectMapper.readValue(body, StockDetailTickMessage.class);

            messagingTemplate.convertAndSend(
                    "/sub/stock/" + tickMessage.stockCode(),
                    tickMessage
            );

            snapshotRepository.save(tickMessage);

        } catch (Exception e) {
            log.warn("StockDetailTickMessage:readValue error", e);
        }
    }
}
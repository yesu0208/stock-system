package arile.toy.stocksystem.bffserver.stockinfo.event;

import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketMainEventSubscriber implements MessageListener {

    private static final String DESTINATION = "/sub/market/main";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            MarketMainResponse response =
                    objectMapper.readValue(message.getBody(), MarketMainResponse.class);
            messagingTemplate.convertAndSend(DESTINATION, response);
        } catch (Exception e) {
            log.error("MarketMain 이벤트 역직렬화 실패", e);
        }
    }
}

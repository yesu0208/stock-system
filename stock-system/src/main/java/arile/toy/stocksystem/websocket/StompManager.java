package arile.toy.stocksystem.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompManager {

    private final SimpMessagingTemplate template;

    public void handleMessage(String code, String message) {
        template.convertAndSend("/sub/stock/" + code, message);
    }
}
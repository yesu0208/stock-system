package arile.toy.stocksystem.websocket;

import arile.toy.stocksystem.websocket.approvalkey.ApprovalKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketManager {

    private final ApprovalKeyService approvalKeyService;
    private final WebSocketClient webSocketClient;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
            String approvalKey = approvalKeyService.issueApprovalKey();
            webSocketClient.connect(approvalKey);
    }
}

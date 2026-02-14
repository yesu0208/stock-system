package arile.toy.stocksystem.stockserver.external.stock.listener;

import arile.toy.stocksystem.stockserver.external.stock.dispatcher.ExternalStockTickMessageDispatcher;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@ClientEndpoint
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalStockWebSocketClient {

    @Value("${api.ws-url}")
    private String WS_URL;
    private Session session;
    private String approvalKey;
    private final ExternalStockTickMessageDispatcher externalStockTickMessageDispatcher;

    public void connect(String approvalKey) {
        try {
            this.approvalKey = approvalKey;
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, URI.create(WS_URL));
        } catch (DeploymentException | IOException exception) {
            throw new IllegalStateException("External stock websocket connection failed.", exception);
        }
    }

    public void disconnect() {
        if (session == null) {
            log.info("External stock websocket already disconnected.");
            return;
        }

        try {
            if (session.isOpen()) {
                session.close(new CloseReason(
                        CloseReason.CloseCodes.NORMAL_CLOSURE,
                        "Client disconnect"
                ));
                log.info("External stock websocket disconnected successfully.");
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "External stock websocket disconnection failed.", e
            );
        } finally {
            session = null;
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @OnOpen
    public void onOpen(Session session){
        log.info("External stock websocket connection success.");
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
//        log.info("Received external stock data: {}", message);

        externalStockTickMessageDispatcher.dispatch(message);
    }

    public void subscribe(String stockCode) {
        if (session == null || !session.isOpen()) {
            log.warn("External stock websocket session not open. Skip subscribe: {}", stockCode);
            return;
        }

        send(buildTradePriceMsg(stockCode));
        send(buildBidAskMsg(stockCode));
    }

    private void send(String msg) {
        session.getAsyncRemote().sendText(msg, result -> {
            if (!result.isOK()) {
                log.error("External stock websocket sending message failed.", result.getException());
            }
        });
    }

    private String buildTradePriceMsg(String stockCode) {
        return """
    {
      "header": {
        "approval_key": "%s",
        "custtype": "P",
        "tr_type": "1",
        "content-type": "utf-8"
      },
      "body": {
        "input": {
          "tr_id": "H0STCNT0",
          "tr_key": "%s"
        }
      }
    }
    """.formatted(approvalKey, stockCode);
    }

    private String buildBidAskMsg(String stockCode) {
        return """
    {
      "header": {
        "approval_key": "%s",
        "custtype": "P",
        "tr_type": "1",
        "content-type": "utf-8"
      },
      "body": {
        "input": {
          "tr_id": "H0STASP0",
          "tr_key": "%s"
        }
      }
    }
    """.formatted(approvalKey, stockCode);
    }
}

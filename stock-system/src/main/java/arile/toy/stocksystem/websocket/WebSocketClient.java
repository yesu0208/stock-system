package arile.toy.stocksystem.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

@RequiredArgsConstructor
@Component
@Slf4j
@ClientEndpoint
public class WebSocketClient {

    @Value("${api.ws-url}")
    private String WS_URL;
    private Session session;
    private String approvalKey;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final StompManager stompManager;

    public void connect(String approvalKey) {
        try {
            this.approvalKey = approvalKey;
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, URI.create(WS_URL));
        } catch (DeploymentException | IOException exception) {
            log.error("WebSocket connect failed", exception);
            throw new IllegalStateException("외부 Stock WebSocket 연결 실패", exception);
        }
    }

    @OnOpen
    public void onOpen(Session session){
        log.info("외부 Stock WebSocket 연결 성공");
        this.session = session;
        subscribe("005930");
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("수신 데이터: {}", message);

        //TODO: parsing data
        if (!message.contains("|")) {

            try {
                JsonNode root = objectMapper.readTree(message);
                if (root.path("body").has("msg1")) {

                    String msg1 = root.path("body").path("msg1").asText();
                    switch (msg1) {
                        case "SUBSCRIBE SUCCESS" -> log.info("SUBSCRIBE SUCCESS");
                        case "UNSUBSCRIBE SUCCESS" -> log.info("UNSUBSCRIBE SUCCESS");
                        case "UNSUBSCRIBE ERROR(not found!)" -> log.warn("UNSUBSCRIBE ERROR(not found!)");
                        default -> log.warn("JSON PARSING ERROR : input not found");
                    }
                    return;

                } else if (root.path("header").path("tr_id").asText().equals("PINGPONG")) { // {"header":{"tr_id":"PINGPONG","datetime":"xxxxxxxxxxxxxx"}}
                    log.info("PINGPONG");
                    return;
                }

            } catch (JsonProcessingException exception) {
                log.error("JSON 변환 실패", exception);
                return;
            }
        }

        String[] parts = message.split("\\|", 4);

//      String encrypted = parts[0];
        String trId = parts[1];
        int count = Integer.parseInt(parts[2]); // 한 메시지에 여러 개의 데이터 들어있을 수 있다
        String payload = parts[3];

        // 복호화(생략)
//        if ("1".equals(encrypted)) {
//            payload = aes256.decrypt(payload, key, iv);
//        }

        String[] fields = payload.split("\\^");
        int fieldSize;
        int offset;

        if (trId.equals("H0STCNT0")) {
            fieldSize = 46;

            for (int i = 0; i < count; i++) {
                offset = i * fieldSize;

                TradePriceTickMessage tradePriceTickMessage = new TradePriceTickMessage(
                        "TRADEPRICE",
                        fields[offset],
                        fields[offset + 1],
                        fields[offset + 2],
                        fields[offset + 4],
                        fields[offset + 7],
                        fields[offset + 8],
                        fields[offset + 9],
                        fields[offset + 12],
                        fields[offset + 13],
                        fields[offset + 14],
                        fields[offset + 19],
                        fields[offset + 20],
                        fields[offset + 21],
                        fields[offset + 41]
                );

                String json;
                try {
                    json = objectMapper.writeValueAsString(tradePriceTickMessage);
                    log.info("JSON 변환 데이터: {}", json);
                    stompManager.handleMessage(fields[offset], json);
                } catch (JsonProcessingException exception) {
                    log.error("JSON 변환 실패", exception);
                }
            }
        } else {
            fieldSize = 59;

            for (int i = 0; i < count; i++) {
                offset = i * fieldSize;

                BidAskPriceTickMessage bidAskPriceTickMessage = new BidAskPriceTickMessage(
                        "BIDASKPRICE",
                        fields[offset+3],
                        fields[offset+4],
                        fields[offset+5],
                        fields[offset+6],
                        fields[offset+7],
                        fields[offset+8],
                        fields[offset+9],
                        fields[offset+10],
                        fields[offset+11],
                        fields[offset+12],
                        fields[offset+13],
                        fields[offset+14],
                        fields[offset+15],
                        fields[offset+16],
                        fields[offset+17],
                        fields[offset+18],
                        fields[offset+19],
                        fields[offset+20],
                        fields[offset+21],
                        fields[offset+22],
                        fields[offset+23],
                        fields[offset+24],
                        fields[offset+25],
                        fields[offset+26],
                        fields[offset+27],
                        fields[offset+28],
                        fields[offset+29],
                        fields[offset+30],
                        fields[offset+31],
                        fields[offset+32],
                        fields[offset+33],
                        fields[offset+34],
                        fields[offset+35],
                        fields[offset+36],
                        fields[offset+37],
                        fields[offset+38],
                        fields[offset+39],
                        fields[offset+40],
                        fields[offset+41],
                        fields[offset+42],
                        fields[offset+43],
                        fields[offset+44]
                );

                String json;
                try {
                    json = objectMapper.writeValueAsString(bidAskPriceTickMessage);
                    log.info("JSON 변환 데이터: {}", json);
                    stompManager.handleMessage(fields[offset], json);
                } catch (JsonProcessingException exception) {
                    log.error("JSON 변환 실패", exception);
                }

            }
        }
    }

    public void subscribe(String symbol) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket session not open. Skip subscribe: {}", symbol);
            return;
        }

        send(buildTradePriceMsg(symbol));
        send(buildBidAskMsg(symbol));
    }

    private void send(String msg) {
        session.getAsyncRemote().sendText(msg, result -> {
            if (!result.isOK()) {
                log.error("WebSocket send failed", result.getException());
            }
        });
    }

    private String buildTradePriceMsg(String symbol) {
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
    """.formatted(approvalKey, symbol);
    }

    private String buildBidAskMsg(String symbol) {
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
    """.formatted(approvalKey, symbol);
    }
}

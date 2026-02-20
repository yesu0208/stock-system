package arile.toy.stocksystem.stockserver.external.stock.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class StateTickMessageHandler {

    private final ObjectMapper objectMapper;

    public void handle(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.path("body").has("msg1")) {

                String msg1 = root.path("body").path("msg1").asText();
                switch (msg1) {
                    case "SUBSCRIBE SUCCESS" -> log.info("External stock websocket subscribe success.");
                    case "UNSUBSCRIBE SUCCESS" -> log.info("External stock websocket unsubscribe success.");
                    case "UNSUBSCRIBE ERROR(not found!)" -> log.warn("External stock websocket unsubscribe error. (not found!)");
                    case "ALREADY IN SUBSCRIBE" -> log.warn("External stock websocket already in subscribe.");
                    default -> log.warn("JSON PARSING ERROR : input not found");
                }

            } else if (root.path("header").path("tr_id").asText().equals("PINGPONG")) { // {"header":{"tr_id":"PINGPONG","datetime":"xxxxxxxxxxxxxx"}}
                log.info("PINGPONG");
            }

        } catch (JsonProcessingException exception) {
            log.warn("JSON modification failed in StateTickMessageHandler.", exception);
        }
    }
}

package arile.toy.stocksystem.stockserver.trade.outbox.service;

import arile.toy.stocksystem.stockserver.trade.event.TradeExecutedEvent;
import arile.toy.stocksystem.stockserver.trade.outbox.entity.TradeOutboxEntity;
import arile.toy.stocksystem.stockserver.trade.outbox.repository.TradeOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeOutboxRecorder {

    private static final String EVENT_TYPE = "TRADE_EXECUTED";

    private final TradeOutboxRepository tradeOutboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * 반드시 체결을 저장하는 트랜잭션과 같은 트랜잭션 컨텍스트 안에서 호출되어야 함.
     * (TradeExecutionService의 @Transactional 메서드 내부에서 호출)
     */
    public void record(TradeExecutedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            tradeOutboxRepository.save(TradeOutboxEntity.of(EVENT_TYPE, payload));
        } catch (Exception e) {
            // 여기서 예외가 나면 전체 트랜잭션이 롤백되어야 하므로 삼키지 않고 던짐
            log.error("Failed to record trade outbox event. event={}", event, e);
            throw new IllegalStateException("Outbox 기록 실패", e);
        }
    }
}

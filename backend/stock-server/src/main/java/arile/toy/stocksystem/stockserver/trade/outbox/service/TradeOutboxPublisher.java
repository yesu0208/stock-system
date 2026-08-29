package arile.toy.stocksystem.stockserver.trade.outbox.service;

import arile.toy.stocksystem.stockserver.trade.outbox.entity.OutboxStatus;
import arile.toy.stocksystem.stockserver.trade.outbox.entity.TradeOutboxEntity;
import arile.toy.stocksystem.stockserver.trade.outbox.repository.TradeOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeOutboxPublisher {

    private final TradeOutboxRepository tradeOutboxRepository;
    private final RedisTemplate<String, Object> streamRedisTemplate;

    @Value("${redis.streams.trade-executed.key}")
    private String streamKey;

    @Scheduled(fixedDelay = 200)
    @Transactional
    public void publishPending() {

        List<TradeOutboxEntity> pendingEvents =
                tradeOutboxRepository.findTop100ByStatusOrderByOutboxIdAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (TradeOutboxEntity outbox : pendingEvents) {
            try {
                Map<String, Object> payload = Map.of(
                        "type", outbox.getEventType(),
                        "payload", outbox.getPayload()
                );

                streamRedisTemplate.opsForStream().add(
                        StreamRecords.mapBacked(payload)
                                .withStreamKey(streamKey)
                );

                outbox.markPublished();

                log.info("Trade outbox event published. outboxId={}", outbox.getOutboxId());

            } catch (Exception e) {
                // 발행 실패한 건은 상태를 바꾸지 않고 다음 폴링 주기에 재시도
                log.error("Failed to publish trade outbox event. outboxId={}", outbox.getOutboxId(), e);
            }
        }

        tradeOutboxRepository.saveAll(pendingEvents);
    }
}

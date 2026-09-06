package arile.toy.stocksystem.stockserver.otoco.event.publisher;

import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.event.OtocoResponseEvent;
import arile.toy.stocksystem.stockserver.otoco.event.StockServerOtocoRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOtocoResponseEventPublisher implements OtocoResponseEventPublisher {

    private final RedisTemplate<String, OtocoResponseEvent> redisOtocoResponseEventRedisTemplate;

    public void publish(StockServerOtocoResponseMessage m) {
        try {
            OtocoResponseEvent event = OtocoResponseEvent.fromMessage(m, true, null);
            redisOtocoResponseEventRedisTemplate.convertAndSend(resolveChannel(event.username()), event);
        } catch (Exception e) {
            log.warn("redisOtocoResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishError(StockServerOtocoRequestEvent request, OtocoResultCode resultCode) {
        try {
            OtocoResponseEvent event = OtocoResponseEvent.of(
                    null, request.username(), request.stockCode(), request.entryDirection(), request.leverageRatio(),
                    request.orderQuantity(), request.entryTriggerPrice(), null, null,
                    OtocoStatus.CANCELED, null, false, resultCode
            );
            redisOtocoResponseEventRedisTemplate.convertAndSend(resolveChannel(event.username()), event);
        } catch (Exception e) {
            log.warn("redisOtocoResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    public void publishEntryTriggered(OtocoDto dto) {
        publishFromDto(dto, true, OtocoResultCode.ENTRY_TRIGGERED);
    }

    public void publishEntryFilled(OtocoDto dto) {
        publishFromDto(dto, true, OtocoResultCode.ENTRY_FILLED);
    }

    public void publishEntryFailed(OtocoDto dto, OtocoResultCode resultCode) {
        publishFromDto(dto, false, resultCode);
    }

    public void publishExitTriggered(OtocoDto dto, OtocoLeg leg) {
        publishFromDto(dto, true, leg == OtocoLeg.TAKE_PROFIT ? OtocoResultCode.TP_TRIGGERED : OtocoResultCode.SL_TRIGGERED);
    }

    public void publishExitFailed(OtocoDto dto, OtocoResultCode resultCode) {
        publishFromDto(dto, false, resultCode);
    }

    private void publishFromDto(OtocoDto dto, boolean success, OtocoResultCode resultCode) {
        try {
            OtocoResponseEvent event = OtocoResponseEvent.of(
                    dto.otocoId(), dto.username(), dto.stockCode(), dto.entryDirection(), dto.leverageRatio(),
                    dto.orderQuantity(), dto.entryTriggerPrice(), dto.tpTriggerPrice(), dto.slTriggerPrice(),
                    dto.otocoStatus(), dto.orderTime(), success, resultCode
            );
            redisOtocoResponseEventRedisTemplate.convertAndSend(resolveChannel(event.username()), event);
        } catch (Exception e) {
            log.warn("redisOtocoResponseEventRedisTemplate.convertAndSend error", e);
        }
    }

    private String resolveChannel(String username) {
        return "user:otoco." + username + ":event";
    }
}

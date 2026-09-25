package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.repository.TrailingStopRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 트레일링 스탑 추적 상태(기준가·발동가)를 주기적으로 DB에 저장.
 * 새 고점·저점은 틱마다 생길 수 있어 틱 처리(종목 락 안)에서 바로 DB에 쓰면 체결 등 다른 틱 처리가 지연되므로,
 * 틱 처리에서는 변경 표시만 하고 별도 스케줄러가 마지막 값만 모아서 저장.
 * 비정상 종료 시 잃는 추적 정보는 최대 한 주기분이며, 재시작 워밍업은 마지막으로 저장된 값에서 추적을 이어감.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrailingStopTrailPersister {

    private final TrailingStopRepository trailingStopRepository;

    // 트레일링 스탑 ID → 아직 저장되지 않은 최신 추적 상태
    private final ConcurrentHashMap<Long, TrailingStopDto> pending = new ConcurrentHashMap<>();

    /** 추적 상태가 바뀐 트레일링 스탑을 저장 대상으로 표시 (같은 ID는 마지막 값만 유지) */
    public void markDirty(TrailingStopDto dto) {
        pending.put(dto.trailingStopId(), dto);
    }

    @Scheduled(fixedDelay = 2000)
    public void flush() {
        for (TrailingStopDto dto : List.copyOf(pending.values())) {
            try {
                trailingStopRepository.updateTrail(
                        dto.trailingStopId(), dto.basePrice(), dto.triggerPrice(), TrailingStopStatus.ACTIVE);
                // 저장하는 사이 더 새로운 값으로 바뀌었으면 제거하지 않고 다음 주기에 다시 저장
                pending.remove(dto.trailingStopId(), dto);
            } catch (Exception e) {
                log.warn("Trailing stop trail persist failed. Will retry next cycle. trailingStopId={}",
                        dto.trailingStopId(), e);
            }
        }
    }

    /** 정상 종료 시 남은 추적 상태를 저장 */
    @PreDestroy
    public void flushOnShutdown() {
        flush();
    }

    int pendingCount() {
        return pending.size();
    }
}

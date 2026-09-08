package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopHistoryItem;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.repository.TrailingStopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrailingStopHistoryQueryService {

    private static final List<TrailingStopStatus> OPEN_STATUSES =
            Arrays.stream(TrailingStopStatus.values()).filter(TrailingStopStatus::isOpen).toList();
    private static final List<TrailingStopStatus> CANCELED_STATUSES = List.of(TrailingStopStatus.CANCELED);
    private static final List<TrailingStopStatus> TRIGGERED_STATUSES = List.of(TrailingStopStatus.TRIGGERED);

    private final TrailingStopRepository trailingStopRepository;

    public HistoryPageResponse<TrailingStopHistoryItem> getHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, null, from, to, pageable);
    }

    public HistoryPageResponse<TrailingStopHistoryItem> getCancelHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, CANCELED_STATUSES, from, to, pageable);
    }

    public HistoryPageResponse<TrailingStopHistoryItem> getUnfilled(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, OPEN_STATUSES, from, to, pageable);
    }

    public HistoryPageResponse<TrailingStopHistoryItem> getTriggeredHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, TRIGGERED_STATUSES, from, to, pageable);
    }

    private HistoryPageResponse<TrailingStopHistoryItem> search(
            String username, String stockCode, List<TrailingStopStatus> statuses,
            Instant from, Instant to, Pageable pageable) {
        Page<TrailingStopEntity> page = trailingStopRepository.search(username, stockCode, statuses, from, to, pageable);
        return HistoryPageResponse.of(page.map(TrailingStopHistoryItem::fromEntity));
    }
}

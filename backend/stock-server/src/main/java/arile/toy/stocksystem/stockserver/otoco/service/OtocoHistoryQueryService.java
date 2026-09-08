package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoHistoryItem;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OtocoHistoryQueryService {

    private static final List<OtocoStatus> OPEN_STATUSES =
            Arrays.stream(OtocoStatus.values()).filter(OtocoStatus::isOpen).toList();
    private static final List<OtocoStatus> CANCELED_STATUSES = List.of(OtocoStatus.CANCELED);
    private static final List<OtocoStatus> COMPLETED_STATUSES = List.of(OtocoStatus.COMPLETED);

    private final OtocoRepository otocoRepository;

    public HistoryPageResponse<OtocoHistoryItem> getHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, null, from, to, pageable);
    }

    public HistoryPageResponse<OtocoHistoryItem> getCancelHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, CANCELED_STATUSES, from, to, pageable);
    }

    public HistoryPageResponse<OtocoHistoryItem> getUnfilled(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, OPEN_STATUSES, from, to, pageable);
    }

    public HistoryPageResponse<OtocoHistoryItem> getCompletedHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, COMPLETED_STATUSES, from, to, pageable);
    }

    private HistoryPageResponse<OtocoHistoryItem> search(
            String username, String stockCode, List<OtocoStatus> statuses,
            Instant from, Instant to, Pageable pageable) {
        Page<OtocoEntity> page = otocoRepository.search(username, stockCode, statuses, from, to, pageable);
        return HistoryPageResponse.of(page.map(OtocoHistoryItem::fromEntity));
    }
}

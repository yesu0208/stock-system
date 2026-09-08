package arile.toy.stocksystem.stockserver.autoorder.sevice;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderHistoryItem;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.repository.AutoOrderRepository;
import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoOrderHistoryQueryService {

    private static final List<AutoOrderStatus> OPEN_STATUSES =
            Arrays.stream(AutoOrderStatus.values()).filter(AutoOrderStatus::isOpen).toList();
    private static final List<AutoOrderStatus> CANCELED_STATUSES = List.of(AutoOrderStatus.CANCELED);
    private static final List<AutoOrderStatus> TRIGGERED_STATUSES = List.of(AutoOrderStatus.TRIGGERED);

    private final AutoOrderRepository autoOrderRepository;

    public HistoryPageResponse<AutoOrderHistoryItem> getHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, null, from, to, pageable);
    }

    public HistoryPageResponse<AutoOrderHistoryItem> getCancelHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, CANCELED_STATUSES, from, to, pageable);
    }

    public HistoryPageResponse<AutoOrderHistoryItem> getUnfilled(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, OPEN_STATUSES, from, to, pageable);
    }

    /** "체결"에 해당: 트리거되어 실제 주문으로 전환된 자동주문. 최종 체결가/수량은 일반주문 /trades API에서 확인 */
    public HistoryPageResponse<AutoOrderHistoryItem> getTriggeredHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, TRIGGERED_STATUSES, from, to, pageable);
    }

    private HistoryPageResponse<AutoOrderHistoryItem> search(
            String username, String stockCode, List<AutoOrderStatus> statuses,
            Instant from, Instant to, Pageable pageable) {
        Page<AutoOrderEntity> page = autoOrderRepository.search(username, stockCode, statuses, from, to, pageable);
        return HistoryPageResponse.of(page.map(AutoOrderHistoryItem::fromEntity));
    }
}

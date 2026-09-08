package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.order.dto.OrderHistoryItem;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import arile.toy.stocksystem.stockserver.trade.dto.TradeHistoryItem;
import arile.toy.stocksystem.stockserver.trade.entity.TradeEntity;
import arile.toy.stocksystem.stockserver.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderHistoryQueryService {

    private static final List<OrderStatus> OPEN_STATUSES =
            Arrays.stream(OrderStatus.values()).filter(OrderStatus::isOpen).toList();
    private static final List<OrderStatus> CANCELED_STATUSES = List.of(OrderStatus.CANCELED);

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;

    public HistoryPageResponse<OrderHistoryItem> getOrderHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, null, from, to, pageable);
    }

    public HistoryPageResponse<OrderHistoryItem> getCancelHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, CANCELED_STATUSES, from, to, pageable);
    }

    public HistoryPageResponse<OrderHistoryItem> getUnfilledOrders(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        return search(username, stockCode, OPEN_STATUSES, from, to, pageable);
    }

    public HistoryPageResponse<TradeHistoryItem> getTradeHistory(
            String username, String stockCode, Instant from, Instant to, Pageable pageable) {
        Page<TradeEntity> page = tradeRepository.search(username, stockCode, from, to, pageable);
        return HistoryPageResponse.of(page.map(TradeHistoryItem::fromEntity));
    }

    private HistoryPageResponse<OrderHistoryItem> search(
            String username, String stockCode, List<OrderStatus> statuses,
            Instant from, Instant to, Pageable pageable) {
        Page<OrderEntity> page = orderRepository.search(username, stockCode, statuses, from, to, pageable);
        return HistoryPageResponse.of(page.map(OrderHistoryItem::fromEntity));
    }
}

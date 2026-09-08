package arile.toy.stocksystem.stockserver.order.controller;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.order.dto.OrderHistoryItem;
import arile.toy.stocksystem.stockserver.order.service.OrderHistoryQueryService;
import arile.toy.stocksystem.stockserver.trade.dto.TradeHistoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderHistoryController {

    private final OrderHistoryQueryService orderHistoryQueryService;

    @GetMapping("/{username}/history")
    public HistoryPageResponse<OrderHistoryItem> getHistory(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderHistoryQueryService.getOrderHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/cancels")
    public HistoryPageResponse<OrderHistoryItem> getCancels(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderHistoryQueryService.getCancelHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/unfilled")
    public HistoryPageResponse<OrderHistoryItem> getUnfilled(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderHistoryQueryService.getUnfilledOrders(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/trades")
    public HistoryPageResponse<TradeHistoryItem> getTrades(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderHistoryQueryService.getTradeHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }
}

package arile.toy.stocksystem.stockserver.autoorder.controller;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderHistoryItem;
import arile.toy.stocksystem.stockserver.autoorder.sevice.AutoOrderHistoryQueryService;
import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/internal/auto-orders")
@RequiredArgsConstructor
public class AutoOrderHistoryController {

    private final AutoOrderHistoryQueryService autoOrderHistoryQueryService;

    @GetMapping("/{username}/history")
    public HistoryPageResponse<AutoOrderHistoryItem> getHistory(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return autoOrderHistoryQueryService.getHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/cancels")
    public HistoryPageResponse<AutoOrderHistoryItem> getCancels(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return autoOrderHistoryQueryService.getCancelHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/unfilled")
    public HistoryPageResponse<AutoOrderHistoryItem> getUnfilled(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return autoOrderHistoryQueryService.getUnfilled(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/triggered")
    public HistoryPageResponse<AutoOrderHistoryItem> getTriggered(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return autoOrderHistoryQueryService.getTriggeredHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }
}

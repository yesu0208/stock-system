package arile.toy.stocksystem.stockserver.trailingstop.controller;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopHistoryItem;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/internal/trailing-stops")
@RequiredArgsConstructor
public class TrailingStopHistoryController {

    private final TrailingStopHistoryQueryService trailingStopHistoryQueryService;

    @GetMapping("/{username}/history")
    public HistoryPageResponse<TrailingStopHistoryItem> getHistory(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return trailingStopHistoryQueryService.getHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/cancels")
    public HistoryPageResponse<TrailingStopHistoryItem> getCancels(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return trailingStopHistoryQueryService.getCancelHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/unfilled")
    public HistoryPageResponse<TrailingStopHistoryItem> getUnfilled(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return trailingStopHistoryQueryService.getUnfilled(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/triggered")
    public HistoryPageResponse<TrailingStopHistoryItem> getTriggered(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return trailingStopHistoryQueryService.getTriggeredHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }
}

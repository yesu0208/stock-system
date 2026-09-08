package arile.toy.stocksystem.stockserver.otoco.controller;

import arile.toy.stocksystem.stockserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoHistoryItem;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/internal/otocos")
@RequiredArgsConstructor
public class OtocoHistoryController {

    private final OtocoHistoryQueryService otocoHistoryQueryService;

    @GetMapping("/{username}/history")
    public HistoryPageResponse<OtocoHistoryItem> getHistory(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return otocoHistoryQueryService.getHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/cancels")
    public HistoryPageResponse<OtocoHistoryItem> getCancels(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return otocoHistoryQueryService.getCancelHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/unfilled")
    public HistoryPageResponse<OtocoHistoryItem> getUnfilled(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return otocoHistoryQueryService.getUnfilled(username, stockCode, from, to, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/completed")
    public HistoryPageResponse<OtocoHistoryItem> getCompleted(
            @PathVariable String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return otocoHistoryQueryService.getCompletedHistory(username, stockCode, from, to, PageRequest.of(page, size));
    }
}

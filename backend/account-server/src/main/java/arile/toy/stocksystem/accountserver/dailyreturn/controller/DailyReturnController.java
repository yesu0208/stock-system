package arile.toy.stocksystem.accountserver.dailyreturn.controller;

import arile.toy.stocksystem.accountserver.dailyreturn.dto.DailyReturnHistoryResponse;
import arile.toy.stocksystem.accountserver.dailyreturn.service.DailyReturnQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal/returns")
@RequiredArgsConstructor
public class DailyReturnController {

    private final DailyReturnQueryService dailyReturnQueryService;

    @GetMapping("/{username}/history")
    public DailyReturnHistoryResponse getHistory(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return dailyReturnQueryService.getHistory(username, from, to, page, size);
    }
}

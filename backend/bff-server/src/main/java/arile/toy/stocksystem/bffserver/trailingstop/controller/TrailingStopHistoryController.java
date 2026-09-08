package arile.toy.stocksystem.bffserver.trailingstop.controller;

import arile.toy.stocksystem.bffserver.admin.service.AdminAccessService;
import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.trailingstop.client.TrailingStopHistoryApiClient;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopHistoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("api/v1/trailing-stops")
@RequiredArgsConstructor
public class TrailingStopHistoryController {

    private final TrailingStopHistoryApiClient trailingStopHistoryApiClient;
    private final AdminAccessService adminAccessService;

    @GetMapping("/history")
    public ResponseEntity<HistoryPageResponse<TrailingStopHistoryItem>> getHistory(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String targetUsername = adminAccessService.resolveTargetUsername(user, username);
        var response = trailingStopHistoryApiClient.getHistory(targetUsername, stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/cancels")
    public ResponseEntity<HistoryPageResponse<TrailingStopHistoryItem>> getCancels(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String targetUsername = adminAccessService.resolveTargetUsername(user, username);
        var response = trailingStopHistoryApiClient.getCancels(targetUsername, stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/unfilled")
    public ResponseEntity<HistoryPageResponse<TrailingStopHistoryItem>> getUnfilled(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String targetUsername = adminAccessService.resolveTargetUsername(user, username);
        var response = trailingStopHistoryApiClient.getUnfilled(targetUsername, stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/triggered")
    public ResponseEntity<HistoryPageResponse<TrailingStopHistoryItem>> getTriggered(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String targetUsername = adminAccessService.resolveTargetUsername(user, username);
        var response = trailingStopHistoryApiClient.getTriggered(targetUsername, stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }
}

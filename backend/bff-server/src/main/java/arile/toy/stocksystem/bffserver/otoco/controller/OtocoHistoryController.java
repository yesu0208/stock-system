package arile.toy.stocksystem.bffserver.otoco.controller;

import arile.toy.stocksystem.bffserver.admin.service.AdminAccessService;
import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.otoco.client.OtocoHistoryApiClient;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoHistoryItem;
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
@RequestMapping("api/v1/otocos")
@RequiredArgsConstructor
public class OtocoHistoryController {

    private final OtocoHistoryApiClient otocoHistoryApiClient;
    private final AdminAccessService adminAccessService;

    @GetMapping("/history")
    public ResponseEntity<HistoryPageResponse<OtocoHistoryItem>> getHistory(
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
        var response = otocoHistoryApiClient.getHistory(targetUsername, stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/cancels")
    public ResponseEntity<HistoryPageResponse<OtocoHistoryItem>> getCancels(
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
        var response = otocoHistoryApiClient.getCancels(targetUsername, stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/unfilled")
    public ResponseEntity<HistoryPageResponse<OtocoHistoryItem>> getUnfilled(
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
        var response = otocoHistoryApiClient.getUnfilled(targetUsername, stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/completed")
    public ResponseEntity<HistoryPageResponse<OtocoHistoryItem>> getCompleted(
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
        var response = otocoHistoryApiClient.getCompleted(targetUsername, stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }
}

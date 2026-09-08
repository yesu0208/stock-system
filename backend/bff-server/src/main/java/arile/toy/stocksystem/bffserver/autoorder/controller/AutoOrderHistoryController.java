// File: bffserver/autoorder/controller/AutoOrderHistoryController.java
package arile.toy.stocksystem.bffserver.autoorder.controller;

import arile.toy.stocksystem.bffserver.autoorder.client.AutoOrderHistoryApiClient;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderHistoryItem;
import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
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
@RequestMapping("api/v1/auto-orders")
@RequiredArgsConstructor
public class AutoOrderHistoryController {

    private final AutoOrderHistoryApiClient autoOrderHistoryApiClient;

    @GetMapping("/history")
    public ResponseEntity<HistoryPageResponse<AutoOrderHistoryItem>> getHistory(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var response = autoOrderHistoryApiClient.getHistory(user.getUsername(), stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/cancels")
    public ResponseEntity<HistoryPageResponse<AutoOrderHistoryItem>> getCancels(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var response = autoOrderHistoryApiClient.getCancels(user.getUsername(), stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/unfilled")
    public ResponseEntity<HistoryPageResponse<AutoOrderHistoryItem>> getUnfilled(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var response = autoOrderHistoryApiClient.getUnfilled(user.getUsername(), stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/triggered")
    public ResponseEntity<HistoryPageResponse<AutoOrderHistoryItem>> getTriggered(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var response = autoOrderHistoryApiClient.getTriggered(user.getUsername(), stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }
}

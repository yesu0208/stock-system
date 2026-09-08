package arile.toy.stocksystem.bffserver.order.controller;

import arile.toy.stocksystem.bffserver.history.dto.HistoryPageResponse;
import arile.toy.stocksystem.bffserver.order.client.OrderHistoryApiClient;
import arile.toy.stocksystem.bffserver.order.dto.OrderHistoryItem;
import arile.toy.stocksystem.bffserver.trade.dto.TradeHistoryItem;
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
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderHistoryController {

    private final OrderHistoryApiClient orderHistoryApiClient;

    @GetMapping("/history")
    public ResponseEntity<HistoryPageResponse<OrderHistoryItem>> getHistory(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var response = orderHistoryApiClient.getHistory(user.getUsername(), stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/cancels")
    public ResponseEntity<HistoryPageResponse<OrderHistoryItem>> getCancels(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var response = orderHistoryApiClient.getCancels(user.getUsername(), stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/unfilled")
    public ResponseEntity<HistoryPageResponse<OrderHistoryItem>> getUnfilled(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var response = orderHistoryApiClient.getUnfilled(user.getUsername(), stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/trades")
    public ResponseEntity<HistoryPageResponse<TradeHistoryItem>> getTrades(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var response = orderHistoryApiClient.getTrades(user.getUsername(), stockCode, from, to, page, size);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }
}

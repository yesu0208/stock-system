package arile.toy.stocksystem.bffserver.watchlist.controller;

import arile.toy.stocksystem.bffserver.watchlist.dto.AddWatchListRequest;
import arile.toy.stocksystem.bffserver.watchlist.dto.WatchListItemResponse;
import arile.toy.stocksystem.bffserver.watchlist.service.WatchListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchListController {

    private final WatchListService watchListService;

    @GetMapping
    public ResponseEntity<List<WatchListItemResponse>> getWatchList(
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var response = watchListService.getAll(user.getUsername()).stream()
                .map(WatchListItemResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WatchListItemResponse> addToWatchList(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody AddWatchListRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var saved = watchListService.add(user.getUsername(), request.stockCode(), request.stockName());

        return ResponseEntity.ok(WatchListItemResponse.fromEntity(saved));
    }

    @DeleteMapping("/{stockCode}")
    public ResponseEntity<Void> removeFromWatchList(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable String stockCode
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        watchListService.remove(user.getUsername(), stockCode);

        return ResponseEntity.noContent().build();
    }
}

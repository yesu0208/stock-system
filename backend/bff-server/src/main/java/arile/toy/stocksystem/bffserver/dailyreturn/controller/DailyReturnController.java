package arile.toy.stocksystem.bffserver.dailyreturn.controller;

import arile.toy.stocksystem.bffserver.dailyreturn.client.DailyReturnApiClient;
import arile.toy.stocksystem.bffserver.dailyreturn.dto.DailyReturnHistoryResponse;
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

import java.time.LocalDate;

@RestController
@RequestMapping("api/v1/users/returns")
@RequiredArgsConstructor
public class DailyReturnController {

    private final DailyReturnApiClient dailyReturnApiClient;

    @GetMapping("/history")
    public ResponseEntity<DailyReturnHistoryResponse> getHistory(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var response = dailyReturnApiClient.getHistory(user.getUsername(), from, to, page, size);

        return response != null ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().build();
    }
}

package arile.toy.stocksystem.bffserver.trailingstopcancel.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelRequest;
import arile.toy.stocksystem.bffserver.trailingstopcancel.dto.TrailingStopCancelResponse;
import arile.toy.stocksystem.bffserver.trailingstopcancel.service.TrailingStopCancelIngressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/trailing-stops/cancel")
@RequiredArgsConstructor
public class TrailingStopCancelController {

    private final TrailingStopCancelIngressService trailingStopCancelIngressService;
    private final BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;

    @PostMapping
    public ResponseEntity<TrailingStopCancelResponse> cancel(
            @Valid @RequestBody TrailingStopCancelRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stockCode = request.stockCode();

        if (bffServerMarketPhaseRegistry.isClosed(stockCode)) {
            throw new MarketClosedException();
        }

        // 요청자를 함께 전달해 stock-server에서 트레일링 스탑 소유자 확인 (타인 트레일링 스탑 취소 방지)
        TrailingStopCancelResponse response =
                trailingStopCancelIngressService.receive(user.getUsername(), request);

        return ResponseEntity.ok(response);
    }
}

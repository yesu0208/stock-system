package arile.toy.stocksystem.bffserver.autocancel.controller;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelRequest;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelResponse;
import arile.toy.stocksystem.bffserver.autocancel.service.AutoCancelIngressService;
import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
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
@RequestMapping("api/v1/auto-orders/cancel")
@RequiredArgsConstructor
public class AutoCancelController {

    private final AutoCancelIngressService autoCancelIngressService;
    private final BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;

    @PostMapping
    public ResponseEntity<AutoCancelResponse> autoCancel(
            @Valid @RequestBody AutoCancelRequest autoCancelRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stockCode = autoCancelRequest.stockCode();

        if (bffServerMarketPhaseRegistry.isClosed(stockCode)) {
            throw new MarketClosedException();
        }

        // 요청자를 함께 전달해 stock-server에서 자동 주문 소유자 확인 (타인 자동 주문 취소 방지)
        AutoCancelResponse autoCancelResponse =
                autoCancelIngressService.receive(user.getUsername(), autoCancelRequest);

        return ResponseEntity.ok(autoCancelResponse);
    }
}

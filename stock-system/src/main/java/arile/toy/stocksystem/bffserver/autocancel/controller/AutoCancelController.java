package arile.toy.stocksystem.bffserver.autocancel.controller;

import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelRequest;
import arile.toy.stocksystem.bffserver.autocancel.dto.AutoCancelResponse;
import arile.toy.stocksystem.bffserver.autocancel.service.AutoCancelIngressService;
import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
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
            @RequestBody AutoCancelRequest autoCancelRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stockCode = autoCancelRequest.stockCode();

        if (bffServerMarketPhaseRegistry.isClosed(stockCode)) {
            throw new MarketClosedException();
        }

        AutoCancelResponse autoCancelResponse =
                autoCancelIngressService.receive(autoCancelRequest);

        return ResponseEntity.ok(autoCancelResponse);
    }
}

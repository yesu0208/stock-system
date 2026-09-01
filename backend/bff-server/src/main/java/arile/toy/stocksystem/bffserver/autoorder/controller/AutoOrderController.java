package arile.toy.stocksystem.bffserver.autoorder.controller;

import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderRequest;
import arile.toy.stocksystem.bffserver.autoorder.dto.AutoOrderResponse;
import arile.toy.stocksystem.bffserver.autoorder.service.AutoOrderIngressService;
import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.order.service.LeverageAccessValidator;
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
@RequestMapping("api/v1/auto-orders")
@RequiredArgsConstructor
public class AutoOrderController {

    private final AutoOrderIngressService autoOrderIngressService;
    private final BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    private final LeverageAccessValidator leverageAccessValidator;

    @PostMapping
    public ResponseEntity<AutoOrderResponse> autoOrder(
            @Valid @RequestBody AutoOrderRequest autoOrderRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stockCode = autoOrderRequest.stockCode();

        if (bffServerMarketPhaseRegistry.isClosed(stockCode)) {
            throw new MarketClosedException();
        }

        String username = user.getUsername();

        leverageAccessValidator.validate(username, autoOrderRequest.leverageRatioOrDefault());

        AutoOrderResponse autoOrderResponse =
                autoOrderIngressService.receive(username, autoOrderRequest);

        return ResponseEntity.ok(autoOrderResponse);
    }
}

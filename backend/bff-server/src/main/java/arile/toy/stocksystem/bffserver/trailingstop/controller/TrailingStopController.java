package arile.toy.stocksystem.bffserver.trailingstop.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.leverage.service.LeverageAccessValidator;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopRequest;
import arile.toy.stocksystem.bffserver.trailingstop.dto.TrailingStopResponse;
import arile.toy.stocksystem.bffserver.trailingstop.service.TrailingStopIngressService;
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
@RequestMapping("api/v1/trailing-stops")
@RequiredArgsConstructor
public class TrailingStopController {

    private final TrailingStopIngressService trailingStopIngressService;
    private final BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    private final LeverageAccessValidator leverageAccessValidator;

    @PostMapping
    public ResponseEntity<TrailingStopResponse> trailingStop(
            @Valid @RequestBody TrailingStopRequest trailingStopRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stockCode = trailingStopRequest.stockCode();

        if (bffServerMarketPhaseRegistry.isClosed(stockCode)) {
            throw new MarketClosedException();
        }

        String username = user.getUsername();

        leverageAccessValidator.validate(username, trailingStopRequest.leverageRatioOrDefault());

        TrailingStopResponse trailingStopResponse =
                trailingStopIngressService.receive(username, trailingStopRequest);

        return ResponseEntity.ok(trailingStopResponse);
    }
}

package arile.toy.stocksystem.bffserver.otoco.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.leverage.service.LeverageAccessValidator;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoRequest;
import arile.toy.stocksystem.bffserver.otoco.dto.OtocoResponse;
import arile.toy.stocksystem.bffserver.otoco.service.OtocoIngressService;
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
@RequestMapping("api/v1/otocos")
@RequiredArgsConstructor
public class OtocoController {

    private final OtocoIngressService otocoIngressService;
    private final BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    private final LeverageAccessValidator leverageAccessValidator;

    @PostMapping
    public ResponseEntity<OtocoResponse> otoco(
            @Valid @RequestBody OtocoRequest otocoRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stockCode = otocoRequest.stockCode();

        if (bffServerMarketPhaseRegistry.isClosed(stockCode)) {
            throw new MarketClosedException();
        }

        String username = user.getUsername();

        leverageAccessValidator.validate(username, otocoRequest.leverageRatioOrDefault());

        OtocoResponse otocoResponse = otocoIngressService.receive(username, otocoRequest);

        return ResponseEntity.ok(otocoResponse);
    }
}

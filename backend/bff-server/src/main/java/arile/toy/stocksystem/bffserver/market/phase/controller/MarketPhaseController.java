package arile.toy.stocksystem.bffserver.market.phase.controller;

import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/market")
@RequiredArgsConstructor
public class MarketPhaseController {

    private final BffServerMarketPhaseRegistry registry;

    @GetMapping("/phase")
    public MarketPhaseResponse getCurrentPhase() {
        return new MarketPhaseResponse(registry.getGlobalPhase().name());
    }
}

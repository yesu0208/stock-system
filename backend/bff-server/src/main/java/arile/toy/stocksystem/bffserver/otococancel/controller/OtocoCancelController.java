package arile.toy.stocksystem.bffserver.otococancel.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelRequest;
import arile.toy.stocksystem.bffserver.otococancel.dto.OtocoCancelResponse;
import arile.toy.stocksystem.bffserver.otococancel.service.OtocoCancelIngressService;
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
@RequestMapping("api/v1/otocos/cancel")
@RequiredArgsConstructor
public class OtocoCancelController {

    private final OtocoCancelIngressService otocoCancelIngressService;
    private final BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;

    @PostMapping
    public ResponseEntity<OtocoCancelResponse> cancel(
            @Valid @RequestBody OtocoCancelRequest otocoCancelRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stockCode = otocoCancelRequest.stockCode();

        if (bffServerMarketPhaseRegistry.isClosed(stockCode)) {
            throw new MarketClosedException();
        }

        // 요청자를 함께 전달해 stock-server에서 OTOCO 소유자 확인 (타인 OTOCO 취소 방지)
        OtocoCancelResponse otocoCancelResponse =
                otocoCancelIngressService.receive(user.getUsername(), otocoCancelRequest);

        return ResponseEntity.ok(otocoCancelResponse);
    }
}

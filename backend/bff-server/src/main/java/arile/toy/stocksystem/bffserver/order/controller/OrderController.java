package arile.toy.stocksystem.bffserver.order.controller;

import arile.toy.stocksystem.bffserver.exception.close.MarketClosedException;
import arile.toy.stocksystem.bffserver.market.phase.BffServerMarketPhaseRegistry;
import arile.toy.stocksystem.bffserver.order.dto.OrderRequest;
import arile.toy.stocksystem.bffserver.order.dto.OrderResponse;
import arile.toy.stocksystem.bffserver.order.service.LeverageAccessValidator;
import arile.toy.stocksystem.bffserver.order.service.OrderIngressService;
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
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderIngressService orderIngressService;
    private final BffServerMarketPhaseRegistry bffServerMarketPhaseRegistry;
    private final LeverageAccessValidator leverageAccessValidator;

    @PostMapping
    public ResponseEntity<OrderResponse> order(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stockCode = orderRequest.stockCode();

        if (bffServerMarketPhaseRegistry.isClosed(stockCode)) {
            throw new MarketClosedException();
        }

        String username = user.getUsername();

        leverageAccessValidator.validate(username, orderRequest.leverageRatioOrDefault());

        OrderResponse orderResponse =
                orderIngressService.receive(username, orderRequest);

        return ResponseEntity.ok(orderResponse);
    }
}

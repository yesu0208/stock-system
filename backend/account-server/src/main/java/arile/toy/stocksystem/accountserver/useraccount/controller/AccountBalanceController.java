package arile.toy.stocksystem.accountserver.useraccount.controller;

import arile.toy.stocksystem.accountserver.useraccount.dto.BalanceCommandResponse;
import arile.toy.stocksystem.accountserver.useraccount.dto.RefundStockRequest;
import arile.toy.stocksystem.accountserver.useraccount.dto.ReserveCashRequest;
import arile.toy.stocksystem.accountserver.useraccount.dto.ReserveStockRequest;
import arile.toy.stocksystem.accountserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.accountserver.useraccount.repository.AccountBalanceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class AccountBalanceController {

    private final AccountBalanceCommand accountBalanceCommand;
    private final AccountUpdateEventPublisher accountUpdateEventPublisher;

    @PostMapping("/{username}/reserve-cash")
    public BalanceCommandResponse reserveCash(
            @PathVariable String username,
            @RequestBody ReserveCashRequest request
    ) {
        boolean success = accountBalanceCommand.reserveCash(username, request.amount());
        publishIfSuccess(username, success);
        return BalanceCommandResponse.of(success);
    }

    @PostMapping("/{username}/refund-cash")
    public BalanceCommandResponse refundCash(
            @PathVariable String username,
            @RequestBody ReserveCashRequest request
    ) {
        boolean success = accountBalanceCommand.refundReservedCash(username, request.amount());
        publishIfSuccess(username, success);
        return BalanceCommandResponse.of(success);
    }

    @PostMapping("/{username}/reserve-stock")
    public BalanceCommandResponse reserveStock(
            @PathVariable String username,
            @RequestBody ReserveStockRequest request
    ) {
        boolean success = accountBalanceCommand.reserveStock(
                username, request.stockCode(), request.quantity());
        publishIfSuccess(username, success);
        return BalanceCommandResponse.of(success);
    }

    @PostMapping("/{username}/refund-stock")
    public BalanceCommandResponse refundStock(
            @PathVariable String username,
            @RequestBody RefundStockRequest request
    ) {
        boolean success = accountBalanceCommand.refundReservedStock(
                username, request.stockCode(), request.quantity());
        publishIfSuccess(username, success);
        return BalanceCommandResponse.of(success);
    }

    private void publishIfSuccess(String username, boolean success) {
        if (success) {
            accountUpdateEventPublisher.publish(username);
        }
    }
}

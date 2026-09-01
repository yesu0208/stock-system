package arile.toy.stocksystem.accountserver.useraccount.controller;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.service.LeveragePositionApplyService;
import arile.toy.stocksystem.accountserver.useraccount.dto.*;
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
    private final LeveragePositionApplyService leveragePositionApplyService;

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

    @PostMapping("/{username}/reserve-leverage-stock")
    public BalanceCommandResponse reserveLeverageStock(
            @PathVariable String username,
            @RequestBody ReserveLeverageStockRequest request
    ) {
        boolean success = leveragePositionApplyService.reserveLeverageStock(
                username, request.stockCode(), LeverageRatio.valueOf(request.leverageRatio()), request.quantity());
        publishIfSuccess(username, success);
        return BalanceCommandResponse.of(success);
    }

    @PostMapping("/{username}/refund-leverage-stock")
    public BalanceCommandResponse refundLeverageStock(
            @PathVariable String username,
            @RequestBody ReserveLeverageStockRequest request
    ) {
        boolean success = leveragePositionApplyService.refundReservedLeverageStock(
                username, request.stockCode(), LeverageRatio.valueOf(request.leverageRatio()), request.quantity());
        publishIfSuccess(username, success);
        return BalanceCommandResponse.of(success);
    }

    private void publishIfSuccess(String username, boolean success) {
        if (success) {
            accountUpdateEventPublisher.publish(username);
        }
    }
}

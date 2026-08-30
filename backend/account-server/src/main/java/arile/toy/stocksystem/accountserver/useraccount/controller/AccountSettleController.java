package arile.toy.stocksystem.accountserver.useraccount.controller;

import arile.toy.stocksystem.accountserver.useraccount.dto.SettleAccountsRequest;
import arile.toy.stocksystem.accountserver.useraccount.service.UserAccountService;
import arile.toy.stocksystem.accountserver.userstock.service.UserStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class AccountSettleController {

    private final UserAccountService userAccountService;
    private final UserStockService userStockService;

    @PostMapping("/settle")
    public ResponseEntity<Void> settle(@RequestBody SettleAccountsRequest request) {
        userAccountService.settleAccounts(request.usernames());
        userStockService.settleStocks(request.usernames());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settle-all")
    public ResponseEntity<Void> settleAll() {
        userAccountService.settleAllAccounts();
        userStockService.settleAllStocks();
        return ResponseEntity.ok().build();
    }
}

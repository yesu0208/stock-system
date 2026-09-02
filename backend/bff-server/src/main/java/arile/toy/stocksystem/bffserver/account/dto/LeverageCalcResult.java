package arile.toy.stocksystem.bffserver.account.dto;

import java.util.List;

public record LeverageCalcResult(
        Long netValue,
        Long equityTotal,
        Long loanTotal,
        Long profitTotal,
        List<LeveragePositionView> views
) {
}

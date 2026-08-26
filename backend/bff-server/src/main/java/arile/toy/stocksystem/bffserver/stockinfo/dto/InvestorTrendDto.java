package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record InvestorTrendDto(

        String dateOrTime,

        long individual,
        long foreigner,
        long institution,

        long financeInvestment,
        long insurance,
        long fund,
        long bank,
        long etcFinance,
        long pension,

        long corporation

) {
}

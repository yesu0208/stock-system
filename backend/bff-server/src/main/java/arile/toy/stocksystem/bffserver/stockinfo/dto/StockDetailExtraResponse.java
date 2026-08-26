package arile.toy.stocksystem.bffserver.stockinfo.dto;

import java.util.List;

public record StockDetailExtraResponse(
        String stockCode,
        String companySummary,
        String warningType,
        String manage,
        List<BrokerTradeInfo> brokerTrades,
        ForeignBrokerSummary foreignBrokerSummary
) {
}
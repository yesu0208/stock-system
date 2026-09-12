package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record BrokerTradeInfo(
        String sellBroker,
        String sellVolume,
        String buyBroker,
        String buyVolume
) {
}

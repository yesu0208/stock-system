package arile.toy.stocksystem.bffserver.stockinfo.dto;

public record ForeignBrokerSummary(
        String name,
        String sellVolume,
        String buyDiff,
        String buyVolume,
        String sellClass,
        String buyDiffClass,
        String buyVolumeClass
) {
}
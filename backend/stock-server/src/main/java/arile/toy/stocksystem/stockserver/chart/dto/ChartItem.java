package arile.toy.stocksystem.stockserver.chart.dto;

public record ChartItem(
        String stck_bsop_date,
        String stck_clpr,
        String stck_oprc,
        String stck_hgpr,
        String stck_lwpr,
        String acml_vol
) {
}

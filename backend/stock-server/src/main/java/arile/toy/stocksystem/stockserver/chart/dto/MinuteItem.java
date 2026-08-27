package arile.toy.stocksystem.stockserver.chart.dto;

public record MinuteItem(
        String stck_bsop_date,
        String stck_cntg_hour,
        String stck_prpr,
        String stck_oprc,
        String stck_hgpr,
        String stck_lwpr,
        String cntg_vol
) {
}

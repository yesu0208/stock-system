package arile.toy.stocksystem.bffserver.chart.dto;

public record ChartItem(

        // 날짜
        String stck_bsop_date,

        // 종가
        String stck_clpr,

        // 시가
        String stck_oprc,

        // 고가
        String stck_hgpr,

        // 저가
        String stck_lwpr,

        // 거래량
        String acml_vol
) {
}

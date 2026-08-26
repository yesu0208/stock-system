package arile.toy.stocksystem.bffserver.chart.service;

import arile.toy.stocksystem.bffserver.chart.dto.CandleData;
import arile.toy.stocksystem.bffserver.chart.dto.ChartResponse;
import arile.toy.stocksystem.bffserver.chart.token.ChartApiTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockChartService {

    @Value("${chart-api.appkey}")
    private String appKey;

    @Value("${chart-api.appsecret}")
    private String appSecret;

    private final WebClient chartApiWebClient;
    private final ChartApiTokenManager chartApiTokenManager;

    public List<CandleData> getDailyChart(
            String stockCode,
            String fromDate,
            String toDate
    ) {

        ChartResponse response = chartApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .queryParam("FID_INPUT_DATE_1", fromDate)
                        .queryParam("FID_INPUT_DATE_2", toDate)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .queryParam("FID_ORG_ADJ_PRC", "0")
                        .build())
                .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + chartApiTokenManager.getAccessToken())
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "FHKST03010100")
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(ChartResponse.class)
                .block();

        if (response == null || response.output2() == null) {
            return List.of();
        }

        return response.output2().stream()
                .map(item -> new CandleData(
                        item.stck_bsop_date(),
                        Long.parseLong(item.stck_oprc()),
                        Long.parseLong(item.stck_hgpr()),
                        Long.parseLong(item.stck_lwpr()),
                        Long.parseLong(item.stck_clpr()),
                        Long.parseLong(item.acml_vol())
                ))
                .toList();
    }
}
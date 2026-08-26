package arile.toy.stocksystem.bffserver.chart.service;

import arile.toy.stocksystem.bffserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.bffserver.chart.dto.MinuteResponse;
import arile.toy.stocksystem.bffserver.chart.token.ChartApiTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMinuteChartService {

    @Value("${chart-api.appkey}")
    private String appKey;

    @Value("${chart-api.appsecret}")
    private String appSecret;

    private final WebClient chartApiWebClient;
    private final ChartApiTokenManager chartApiTokenManager;

    public List<MinuteCandle> getMinuteChart(
            String stockCode,
            String date,
            String hour
    ) {

        MinuteResponse response = chartApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .queryParam("FID_INPUT_DATE_1", date)
                        .queryParam("FID_INPUT_HOUR_1", hour)
                        .queryParam("FID_PW_DATA_INCU_YN", "Y")
                        .queryParam("FID_FAKE_TICK_INCU_YN", "N")
                        .build())
                .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + chartApiTokenManager.getAccessToken())
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "FHKST03010230")
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(MinuteResponse.class)
                .block();

        if (response == null || response.output2() == null) {
            return List.of();
        }

        return response.output2().stream()
                .map(i -> new MinuteCandle(
                        i.stck_bsop_date(),
                        i.stck_cntg_hour(),
                        Long.parseLong(i.stck_oprc()),
                        Long.parseLong(i.stck_hgpr()),
                        Long.parseLong(i.stck_lwpr()),
                        Long.parseLong(i.stck_prpr()),
                        Long.parseLong(i.cntg_vol())
                ))
                .toList();
    }
}
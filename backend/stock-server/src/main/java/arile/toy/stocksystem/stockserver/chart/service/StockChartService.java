package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.dto.CandleData;
import arile.toy.stocksystem.stockserver.chart.dto.ChartResponse;
import arile.toy.stocksystem.stockserver.chart.token.ChartApiTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockChartService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

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
        // 날짜 순서 보정 (혹시 from > to 로 들어온 경우 대비)
        if (fromDate.compareTo(toDate) > 0) {
            String tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        LocalDate from = LocalDate.parse(fromDate, DATE_FORMAT);
        LocalDate currentTo = LocalDate.parse(toDate, DATE_FORMAT);

        List<CandleData> result = new ArrayList<>();

        while (!currentTo.isBefore(from)) {

            final String requestFrom = fromDate; // API 스펙상 FID_INPUT_DATE_1은 항상 원래 from으로 보내도 되지만,
            final String requestTo = currentTo.format(DATE_FORMAT);

            ChartResponse response = requestDailyChart(stockCode, requestFrom, requestTo);

            if (response == null || response.output2() == null || response.output2().isEmpty()) {
                break;
            }

            List<CandleData> batch = response.output2().stream()
                    .map(item -> new CandleData(
                            item.stck_bsop_date(),
                            Long.parseLong(item.stck_oprc()),
                            Long.parseLong(item.stck_hgpr()),
                            Long.parseLong(item.stck_lwpr()),
                            Long.parseLong(item.stck_clpr()),
                            Long.parseLong(item.acml_vol())
                    ))
                    .toList();

            result.addAll(batch);

            // 이번 배치에서 가장 오래된 날짜를 찾아 다음 요청의 기준점으로 사용
            String oldestDate = batch.get(batch.size() - 1).date();
            LocalDate oldest = LocalDate.parse(oldestDate, DATE_FORMAT);

            LocalDate nextTo = oldest.minusDays(1);

            // 더 이상 진행이 안 되는 경우(무한루프 방지) 종료
            if (!nextTo.isBefore(currentTo)) {
                break;
            }
            currentTo = nextTo;

            // 받은 데이터가 페이지 크기(100)보다 적으면 더 가져올 게 없다는 뜻
            if (batch.size() < 100) {
                break;
            }
        }

        // 중복 제거 + 날짜 오름차순 정렬
        return result.stream()
                .collect(
                        LinkedHashMap<String, CandleData>::new,
                        (map, c) -> map.put(c.date(), c),
                        LinkedHashMap::putAll
                )
                .values()
                .stream()
                .sorted(Comparator.comparing(CandleData::date))
                .toList();
    }

    private ChartResponse requestDailyChart(String stockCode, String from, String to) {
        return chartApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .queryParam("FID_INPUT_DATE_1", from)
                        .queryParam("FID_INPUT_DATE_2", to)
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
    }
}

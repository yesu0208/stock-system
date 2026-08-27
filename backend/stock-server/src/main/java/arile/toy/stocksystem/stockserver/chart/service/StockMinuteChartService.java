package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.stockserver.chart.dto.MinuteResponse;
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
public class StockMinuteChartService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    // 정규장 시간 (필요시 동시호가/시간외 포함 여부에 맞게 조정)
    private static final String MARKET_OPEN_TIME = "090000";
    private static final String MARKET_CLOSE_TIME = "153000";

    @Value("${chart-api.appkey}")
    private String appKey;

    @Value("${chart-api.appsecret}")
    private String appSecret;

    private final WebClient chartApiWebClient;
    private final ChartApiTokenManager chartApiTokenManager;

    public List<MinuteCandle> getMinuteChart(
            String stockCode,
            String date,
            String hour,
            int count
    ) {
        List<MinuteCandle> result = new ArrayList<>();

        String currentDate = date;
        String currentHour = hour;

        while (result.size() < count) {

            MinuteResponse response = requestMinuteChart(stockCode, currentDate, currentHour);

            if (response == null || response.output2() == null || response.output2().isEmpty()) {
                // 해당 날짜에 더 이상 데이터가 없으면 전 거래일로 점프해서 재시도
                String[] jumped = jumpToPreviousTradingSession(currentDate);
                if (jumped[0].equals(currentDate)) {
                    break; // 더 이상 진행 불가
                }
                currentDate = jumped[0];
                currentHour = jumped[1];
                continue;
            }

            List<MinuteCandle> batch = response.output2().stream()
                    .filter(i -> i.stck_bsop_date() != null && !i.stck_bsop_date().isBlank())
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

            if (batch.isEmpty()) {
                String[] jumped = jumpToPreviousTradingSession(currentDate);
                if (jumped[0].equals(currentDate)) break;
                currentDate = jumped[0];
                currentHour = jumped[1];
                continue;
            }

            result.addAll(batch);

            String oldestDate = batch.get(batch.size() - 1).date();
            String oldestHour = batch.get(batch.size() - 1).time();

            // 다음 조회 시각 계산 (장 시작 시간 이전으로 내려가면 전 거래일 종가시간으로 점프)
            String[] next = previousTradingTimestamp(oldestDate, oldestHour);
            currentDate = next[0];
            currentHour = next[1];

            if (batch.size() < 100) {
                // 이번 배치가 마지막 페이지였다면, 해당 날짜에서는 더 받을 게 없으니
                // 다음 루프에서 자연히 전 거래일로 점프하도록 그대로 진행
            }
        }

        return result.stream()
                .collect(
                        LinkedHashMap<String, MinuteCandle>::new,
                        (map, c) -> map.put(c.date() + c.time(), c),
                        LinkedHashMap::putAll
                )
                .values()
                .stream()
                .sorted(
                        Comparator.comparing(MinuteCandle::date)
                                .thenComparing(MinuteCandle::time)
                )
                .toList();
    }

    private MinuteResponse requestMinuteChart(String stockCode, String date, String hour) {
        return chartApiWebClient.get()
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
    }

    /**
     * date+time 기준 1초 전 시각을 구하되,
     * 장 시작(09:00:00) 이전으로 내려가면 08:59:00 ~ 전일 15:31:00 구간은 건너뛰고
     * 바로 전 거래일의 장 마감(15:30:00)으로 점프한다.
     */
    private String[] previousTradingTimestamp(String date, String hhmmss) {
        String decremented = decrementSecond(hhmmss);

        if (decremented.compareTo(MARKET_OPEN_TIME) < 0) {
            return jumpToPreviousTradingSession(date);
        }
        return new String[]{date, decremented};
    }

    private String[] jumpToPreviousTradingSession(String date) {
        LocalDate prevDate = LocalDate.parse(date, DATE_FORMAT).minusDays(1);
        // TODO: 주말/공휴일 스킵이 필요하면 여기서 캘린더 체크 로직 추가
        return new String[]{prevDate.format(DATE_FORMAT), MARKET_CLOSE_TIME};
    }

    private String decrementSecond(String hhmmss) {
        int h = Integer.parseInt(hhmmss.substring(0, 2));
        int m = Integer.parseInt(hhmmss.substring(2, 4));
        int s = Integer.parseInt(hhmmss.substring(4, 6));

        int total = h * 3600 + m * 60 + s - 1;
        if (total < 0) total = 0;

        h = total / 3600;
        m = (total % 3600) / 60;
        s = total % 60;

        return String.format("%02d%02d%02d", h, m, s);
    }
}

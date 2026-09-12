package arile.toy.stocksystem.bffserver.stockinfo.client;

import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.core.ParameterizedTypeReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.RoundingMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class NaverStockCrawlerClient {

    private final RestClient restClient;
    private final RestClient stockApiClient;

    public NaverStockCrawlerClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://finance.naver.com")
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .build();

        this.stockApiClient = RestClient.builder()
                .baseUrl("https://stock.naver.com")
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    public StockInfo getStockInfo(String code) {
        try {
            String html = getHtml(code);
            return parse(html);
        } catch (RestClientResponseException e) {
            log.error("Naver stock crawling error. status={}, code={}", e.getStatusCode(), code);
            throw new IllegalStateException("네이버 종목 정보 크롤링 실패", e);
        }
    }

    private String getHtml(String code) {
        return restClient.get()
                .uri("/item/main.naver?code={code}", code)
                .retrieve()
                .body(String.class);
    }

    private StockInfo parse(String html) {
        Document doc = Jsoup.parse(html);

        String marketCap = doc.select("#_market_sum").text();
        String marketCapRank = getValueByTh(doc, "시가총액순위");
        String listedShares = getValueByTh(doc, "상장주식수");

        String[] parTradingSplit = splitBar(getValueByTh(doc, "액면가l매매단위"));
        String foreignLimit = getValueByTh(doc, "외국인한도주식수(A)");
        String foreignOwned = getValueByTh(doc, "외국인보유주식수(B)");
        String foreignRate = getValueByTh(doc, "외국인소진율(B/A)");

        String[] opinionSplit = splitBar(getValueByTh(doc, "투자의견l목표주가"));
        String[] highLowSplit = splitBar(getValueByTh(doc, "52주최고l최저"));
        String[] perEpsSplit = splitBar(getValueByThContains(doc, "PERlEPS"));
        String[] estimatedSplit = splitBar(getValueByThContains(doc, "추정PERlEPS"));
        String[] pbrSplit = splitBar(getValueByThContains(doc, "PBRlBPS"));

        String dividendYield = getValueByThContains(doc, "배당수익률");
        String sameIndustryPer = getValueByTh(doc, "동일업종 PER");
        String sameIndustryRate = getValueByTh(doc, "동일업종 등락률");

        return new StockInfo(
                marketCap, marketCapRank, listedShares,
                parTradingSplit[0], parTradingSplit[1],
                foreignLimit, foreignOwned, foreignRate,
                opinionSplit[0], opinionSplit[1],
                highLowSplit[0], highLowSplit[1],
                perEpsSplit[0], perEpsSplit[1],
                estimatedSplit[0], estimatedSplit[1],
                pbrSplit[0], pbrSplit[1],
                dividendYield,
                sameIndustryPer, sameIndustryRate
        );
    }

    private String getValueByTh(Document doc, String thText) {
        String target = thText.replace(" ", "");
        return findRowValue(doc, thValue -> thValue.contains(target));
    }

    private String getValueByThContains(Document doc, String keyword) {
        return findRowValue(doc, thValue -> thValue.contains(keyword));
    }

    private String findRowValue(Document doc, Predicate<String> matcher) {
        Elements rows = doc.select("tr");

        for (Element row : rows) {
            Element th = row.selectFirst("th");
            Element td = row.selectFirst("td");

            if (th != null && td != null) {
                String thValue = th.text().replace(" ", "").replace("|", "l");
                if (matcher.test(thValue)) {
                    return td.text();
                }
            }
        }
        return "";
    }

    private String[] splitBar(String text) {
        String[] split = text.split("l");
        if (split.length < 2) {
            return new String[]{text, ""};
        }
        return new String[]{split[0].trim(), split[1].trim()};
    }



    private static final int FOREIGN_TRADE_PAGE_SIZE = 10;

    public TradePageResponse getForeignInstitutionTrades(String code, int page) {

        List<ForeignInstitutionTrade> current = fetchTradePage(code, page);
        List<ForeignInstitutionTrade> next = fetchTradePage(code, page + 1);

        boolean hasNext = !next.isEmpty();

        return new TradePageResponse(current, hasNext);
    }

    private List<ForeignInstitutionTrade> fetchTradePage(String code, int page) {

        int startIdx = page - 1;

        List<NaverForeignTrendItem> items;
        try {
            items = stockApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/domestic/detail/{code}/trend")
                            .queryParam("tradeType", "KRX")
                            .queryParam("startIdx", startIdx)
                            .queryParam("pageSize", FOREIGN_TRADE_PAGE_SIZE)
                            .build(code))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NaverForeignTrendItem>>() {});
        } catch (RestClientResponseException e) {
            log.error("Naver 외국인/기관 매매동향 API 호출 실패. status={}, code={}, page={}",
                    e.getStatusCode(), code, page);
            throw new IllegalStateException("네이버 외국인/기관 매매동향 크롤링 실패", e);
        }

        if (items == null) {
            return new ArrayList<>();
        }

        List<ForeignInstitutionTrade> result = new ArrayList<>();
        for (NaverForeignTrendItem item : items) {
            result.add(mapForeignTrend(item));
        }

        return result;
    }

    private ForeignInstitutionTrade mapForeignTrend(NaverForeignTrendItem item) {

        String date = formatBizDate(item.bizdate());
        String closePrice = formatComma(item.closePrice());
        String diff = formatComma(String.valueOf(Math.abs(parseLongSafely(item.prevChangePrice()))));
        String direction = mapUpDownGbText(item.upDownGb());
        String rate = formatRate(item.closePrice(), item.prevChangePrice());
        String volume = formatComma(item.tradeVolume());
        String institutionNetBuy = formatSignedComma(item.organPureBuyQuant());
        String individualNetBuy = formatSignedComma(item.individualPureBuyQuant());
        String foreignNetBuy = formatSignedComma(item.foreignerPureBuyQuant());
        String foreignHoldings = formatComma(item.frgnStock());
        String foreignRate = formatPercent(item.frgnHoldRatio());

        return new ForeignInstitutionTrade(
                date, closePrice, diff, direction, rate,
                volume, institutionNetBuy, individualNetBuy,
                foreignNetBuy, foreignHoldings, foreignRate
        );
    }

    private String mapUpDownGbText(String upDownGb) {
        if (upDownGb == null) {
            return "UNKNOWN";
        }
        return switch (upDownGb) {
            case "상한가" -> "UPPER_LIMIT";
            case "상승" -> "UP";
            case "보합" -> "STEADY";
            case "하한가" -> "LOWER_LIMIT";
            case "하락" -> "DOWN";
            default -> "UNKNOWN";
        };
    }

    private String formatBizDate(String bizdate) {
        if (bizdate == null || bizdate.length() != 8) {
            return bizdate == null ? "" : bizdate;
        }
        return bizdate.substring(0, 4) + "." + bizdate.substring(4, 6) + "." + bizdate.substring(6, 8);
    }

    private String formatDiff(String upDownGb, String prevChangePriceRaw) {

        long prevChangePrice = parseLongSafely(prevChangePriceRaw);
        String absValue = formatComma(String.valueOf(Math.abs(prevChangePrice)));

        if (upDownGb == null) {
            return absValue;
        }

        return switch (upDownGb) {
            case "상승" -> "▲ " + absValue;
            case "하락" -> "▼ " + absValue;
            case "상한가" -> "⬆" + absValue;
            case "하한가" -> "⬇" + absValue;
            default -> "0";
        };
    }

    private String formatRate(String closePriceRaw, String prevChangePriceRaw) {

        try {
            BigDecimal closePrice = new BigDecimal(closePriceRaw);
            BigDecimal prevChangePrice = new BigDecimal(prevChangePriceRaw);
            BigDecimal prevClosePrice = closePrice.subtract(prevChangePrice);

            if (prevClosePrice.compareTo(BigDecimal.ZERO) == 0) {
                return "0.00%";
            }

            BigDecimal rate = prevChangePrice
                    .divide(prevClosePrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            String sign = rate.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
            return sign + rate + "%";
        } catch (Exception e) {
            log.warn("등락률 계산 실패. closePrice={}, prevChangePrice={}", closePriceRaw, prevChangePriceRaw, e);
            return "0.00%";
        }
    }

    private String formatComma(String rawNumber) {
        try {
            long value = Long.parseLong(rawNumber.trim());
            return String.format("%,d", Math.abs(value));
        } catch (Exception e) {
            return rawNumber == null ? "" : rawNumber;
        }
    }

    private String formatSignedComma(String rawNumber) {
        try {
            long value = Long.parseLong(rawNumber.trim());
            String formatted = String.format("%,d", Math.abs(value));
            return value < 0 ? "-" + formatted : formatted;
        } catch (Exception e) {
            return rawNumber == null ? "" : rawNumber;
        }
    }

    private String formatPercent(String rawPercent) {
        try {
            BigDecimal value = new BigDecimal(rawPercent).setScale(2, RoundingMode.HALF_UP);
            return value + "%";
        } catch (Exception e) {
            return rawPercent == null ? "" : rawPercent;
        }
    }

    private long parseLongSafely(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    public MarketMainResponse getMarketIndices() {

        Document doc = fetchSiseDocument();

        MarketIndexInfo kospi = parseMarketIndex(doc, "KOSPI", "코스피", false);
        MarketIndexInfo kosdaq = parseMarketIndex(doc, "KOSDAQ", "코스닥", false);
        MarketIndexInfo kospi200 = parseMarketIndex(doc, "KPI200", "코스피200", true);

        return new MarketMainResponse(kospi, kosdaq, kospi200);
    }

    public List<PopularStock> getPopularStocks() {

        List<NaverPopularStockItem> items;
        try {
            items = stockApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/domestic/market/stock/default")
                            .queryParam("tradeType", "KRX")
                            .queryParam("marketType", "ALL")
                            .queryParam("orderType", "searchTop")
                            .queryParam("startIdx", 0)
                            .queryParam("pageSize", 30)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NaverPopularStockItem>>() {});
        } catch (RestClientResponseException e) {
            log.error("Naver 인기 종목 API 호출 실패. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 인기 종목 조회 실패", e);
        }

        if (items == null) {
            throw new IllegalStateException("네이버 인기 종목 응답이 비어 있습니다.");
        }

        List<PopularStock> result = new ArrayList<>();
        int rank = 1;
        for (NaverPopularStockItem item : items) {
            result.add(new PopularStock(
                    rank++,
                    item.itemcode(),
                    item.itemname(),
                    item.nowPrice(),
                    mapDirection(item.upDownGb())
            ));
        }

        return result;
    }

    private String mapDirection(String upDownGb) {
        if (upDownGb == null) {
            return "UNKNOWN";
        }
        return switch (upDownGb) {
            case "1" -> "UPPER_LIMIT";
            case "2" -> "UP";
            case "3" -> "STEADY";
            case "4" -> "LOWER_LIMIT";
            case "5" -> "DOWN";
            default -> "UNKNOWN";
        };
    }

    private Document fetchSiseDocument() {
        try {
            String html = restClient.get()
                    .uri("/sise/")
                    .retrieve()
                    .body(String.class);

            return Jsoup.parse(html);
        } catch (RestClientResponseException e) {
            log.error("Naver market main crawling error. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 증시 메인 크롤링 실패", e);
        }
    }

    private MarketIndexInfo parseMarketIndex(Document doc, String code, String name, boolean isKospi200) {

        String current = doc.select("#" + code + "_now").text();

        Element changeEl = doc.selectFirst("#" + code + "_change");
        String changeText = changeEl.text();

        String direction = "";
        if (!changeEl.select(".nup").isEmpty()) {
            direction = "UP";
        } else if (!changeEl.select(".ndown").isEmpty()) {
            direction = "DOWN";
        }

        String[] split = splitChange(changeText);
        String changeValue = split[0];
        
        String changeRate = split[1].replaceAll("(%).*", "$1").trim();

        String timeId = switch (code) {
            case "KOSPI" -> "#time1";
            case "KOSDAQ" -> "#time2";
            default -> "#time3";
        };

        String baseTime = doc.select(timeId).text()
                .replace("장마감", " 장마감")
                .replace("장중", " 장중");

        MarketBreadth breadth;

        if (isKospi200) {
            String basis = doc.select("#kpi200_basis").text()
                    .replace("콘탱고", "")
                    .replace("백워데이션", "")
                    .trim();

            breadth = new MarketBreadth(null, null, null, null, null, basis);
        } else {
            String panelId = code.equals("KOSPI") ? "#tab_sel1_risefall" : "#tab_sel2_risefall";
            Elements stockDds = doc.select(panelId + " dl.stock dd");

            breadth = new MarketBreadth(
                    stockDds.get(0).text(),
                    stockDds.get(1).text(),
                    stockDds.get(2).text(),
                    stockDds.get(3).text(),
                    stockDds.get(4).text(),
                    null
            );
        }

        String trendId = switch (code) {
            case "KOSPI" -> "#tab_sel1_risefall";
            case "KOSDAQ" -> "#tab_sel2_risefall";
            default -> "#tab_sel3_risefall";
        };

        Elements trendDds = doc.select(trendId + " dl.trend dd");

        ProgramTrade programTrade = new ProgramTrade(
                cleanProgramTrade(trendDds.get(0).text()),
                cleanProgramTrade(trendDds.get(1).text()),
                cleanProgramTrade(trendDds.get(2).text())
        );

        String trendSelector = switch (code) {
            case "KOSPI" -> "#tab_sel1_deal_trend";
            case "KOSDAQ" -> "#tab_sel2_deal_trend";
            default -> "#tab_sel3_deal_trend";
        };

        Elements investorItems = doc.select(trendSelector + " li");

        String personal = investorItems.get(1).select(".val").text();
        String foreigner = investorItems.get(2).select(".val").text();
        String institution = investorItems.get(3).select(".val").text();

        InvestorTrend investorTrend = new InvestorTrend(personal, foreigner, institution);

        return new MarketIndexInfo(
                name,
                current,
                changeValue,
                changeRate,
                direction,
                baseTime,
                breadth,
                programTrade,
                investorTrend
        );
    }

    private String cleanProgramTrade(String text) {
        return text
                .replace("비차익 ", "")
                .replace("차익 ", "")
                .replace("전체 ", "")
                .trim();
    }

    private String[] splitChange(String text) {

        String[] tokens = text.split(" ");

        String value = "";
        String rate = "";

        for (String token : tokens) {
            if (token.contains("%")) {
                rate = token;
            } else if (token.matches("^[+-]?[0-9.,]+$")) {
                value = token;
            }
        }

        return new String[]{value, rate};
    }

    public UpjongResponse getAllUpjongs() {

        NaverIndustryRankingResponse response;
        try {
            response = stockApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/stockSecurity/rankings/v2/domestic/industries")
                            .queryParam("sortType", "changeRate")
                            .queryParam("size", 100)
                            .queryParam("period", "daily")
                            .build())
                    .retrieve()
                    .body(NaverIndustryRankingResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 업종 목록 API 호출 실패. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 업종 목록 크롤링 실패", e);
        }

        if (response == null || response.items() == null) {
            throw new IllegalStateException("네이버 업종 목록 응답이 비어 있습니다.");
        }

        double maxAbsChangeRate = response.items().stream()
                .mapToDouble(item -> Math.abs(parseDoubleSafely(item.changeRate())))
                .max()
                .orElse(0.0);

        List<UpjongInfo> result = new ArrayList<>();
        for (NaverIndustryItem item : response.items()) {
            result.add(mapUpjongInfo(item, maxAbsChangeRate));
        }

        return new UpjongResponse(result);
    }

    private UpjongInfo mapUpjongInfo(NaverIndustryItem item, double maxAbsChangeRate) {

        long rise = parseLongSafely(item.risingCount());
        long fall = parseLongSafely(item.fallingCount());
        long steady = parseLongSafely(item.unchangedCount());
        long total = rise + fall + steady;

        double absChangeRate = Math.abs(parseDoubleSafely(item.changeRate()));
        String graphRatio = maxAbsChangeRate > 0
                ? String.valueOf(Math.round((absChangeRate / maxAbsChangeRate) * 100.0))
                : "0";

        return new UpjongInfo(
                item.name(),
                item.code(),
                item.changeRate(),
                String.valueOf(total),
                item.risingCount(),
                item.unchangedCount(),
                item.fallingCount(),
                graphRatio,
                item.totalMarketCap(),
                item.totalTradingVolume(),
                item.totalTradingValue(),
                mapTop3(item.topByChangeRate()),
                mapTop3(item.topByMarketCap()),
                mapTop3(item.topByTradingValue())
        );
    }

    private double parseDoubleSafely(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private UpjongInfo mapUpjongInfo(NaverIndustryItem item) {

        long rise = parseLongSafely(item.risingCount());
        long fall = parseLongSafely(item.fallingCount());
        long steady = parseLongSafely(item.unchangedCount());
        long total = rise + fall + steady;

        String graphRatio = total > 0
                ? String.valueOf(Math.round((rise * 100.0) / total))
                : "0";

        return new UpjongInfo(
                item.name(),
                item.code(),
                item.changeRate(),
                String.valueOf(total),
                item.risingCount(),
                item.unchangedCount(),
                item.fallingCount(),
                graphRatio,
                item.totalMarketCap(),
                item.totalTradingVolume(),
                item.totalTradingValue(),
                mapTop3(item.topByChangeRate()),
                mapTop3(item.topByMarketCap()),
                mapTop3(item.topByTradingValue())
        );
    }

    private List<UpjongRankItem> mapTop3(List<NaverIndustryRankEntry> entries) {

        if (entries == null) {
            return new ArrayList<>();
        }

        List<UpjongRankItem> result = new ArrayList<>();
        for (int i = 0; i < Math.min(3, entries.size()); i++) {
            NaverIndustryRankEntry e = entries.get(i);
            result.add(new UpjongRankItem(e.code(), e.name(), e.value(), e.itemLogoUrl()));
        }
        return result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverIndustryRankingResponse(
            boolean hasNext,
            List<NaverIndustryItem> items,
            String cursor
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverIndustryItem(
            String ranking,
            String code,
            String name,
            String changeRate,
            String risingCount,
            String fallingCount,
            String unchangedCount,
            String totalMarketCap,
            String totalTradingVolume,
            String totalTradingValue,
            List<NaverIndustryRankEntry> topByChangeRate,
            List<NaverIndustryRankEntry> topByMarketCap,
            List<NaverIndustryRankEntry> topByTradingValue,
            List<NaverIndustryRankEntry> topByTradingVolume,
            String updatedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverIndustryRankEntry(
            String code,
            String name,
            String value,
            String itemLogoUrl
    ) {
    }

    public List<UpjongStock> getUpjongStocks(String upjongNo) {

        List<NaverUpjongStockItem> items;
        try {
            items = stockApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/domestic/market/upjong/{no}/stocklist")
                            .queryParam("marketType", "ALL")
                            .queryParam("orderType", "priceTop")
                            .queryParam("startIdx", 0)
                            .queryParam("pageSize", 200)
                            .build(upjongNo))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NaverUpjongStockItem>>() {});
        } catch (RestClientResponseException e) {
            log.error("Naver 업종별 종목 API 호출 실패. status={}, no={}", e.getStatusCode(), upjongNo);
            throw new IllegalStateException("네이버 업종별 종목 크롤링 실패", e);
        }

        if (items == null) {
            throw new IllegalStateException("네이버 업종별 종목 응답이 비어 있습니다.");
        }

        List<UpjongStock> result = new ArrayList<>();
        for (NaverUpjongStockItem item : items) {
            result.add(mapUpjongStock(item));
        }

        return result;
    }

    private UpjongStock mapUpjongStock(NaverUpjongStockItem item) {

        String price = formatComma(item.nowPrice());
        String change = formatComma(item.prevChangePrice());
        String direction = mapDirection(item.upDownGb());
        String rate = formatSignedRate(item.prevChangeRate());
        String volume = formatComma(item.tradeVolume());
        String tradingValue = formatComma(item.tradeAmount());
        String marketCap = formatComma(item.marketSum());

        return new UpjongStock(
                item.itemname(),
                item.itemcode(),
                price,
                change,
                direction,
                rate,
                volume,
                tradingValue,
                marketCap
        );
    }

    private String formatUpDownChange(String upDownGb, String prevChangePriceRaw) {

        long prevChangePrice = parseLongSafely(prevChangePriceRaw);
        String absValue = formatComma(String.valueOf(Math.abs(prevChangePrice)));

        if (upDownGb == null) {
            return absValue;
        }

        return switch (upDownGb) {
            case "1" -> "⬆" + absValue;
            case "2" -> "▲ " + absValue;
            case "3" -> absValue;
            case "4" -> "⬇" + absValue;
            case "5" -> "▼ " + absValue;
            default -> absValue;
        };
    }

    private String formatSignedRate(String rawRate) {
        try {
            BigDecimal value = new BigDecimal(rawRate).setScale(2, RoundingMode.HALF_UP);
            String sign = value.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
            return sign + value + "%";
        } catch (Exception e) {
            return rawRate == null ? "" : rawRate;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverUpjongStockItem(
            String itemcode,
            String itemname,
            String nowPrice,
            String upDownGb,
            String prevChangePrice,
            String prevChangeRate,
            String tradeVolume,
            String tradeAmount,
            String marketSum
    ) {
    }

    // 종목 상세 (5초 브로드캐스트용)

    public StockDetailTickMessage getStockDetailSummary(String code) {

        Document doc = fetchDetailDocument(code);

        Element blind = doc.selectFirst("dl.blind");
        if (blind == null) {
            throw new IllegalStateException("네이버 종목 상세 파싱 실패: dl.blind 없음. code=" + code);
        }

        Elements dds = blind.select("dd");
        if (dds.size() < 12) {
            throw new IllegalStateException("네이버 종목 상세 파싱 실패: dd 개수 부족. code=" + code);
        }

        String baseTime = dds.get(0).text();

        String stockName = extractAfter(dds.get(1).text(), "종목명");

        String codeMarket = dds.get(2).text();
        String market = codeMarket.contains("코스닥") ? "코스닥" : "코스피";

        PriceInfo priceInfo = parsePriceInfo(dds.get(3).text());

        String prevPrice = extractAfter(dds.get(4).text(), "전일가");
        String openPrice = extractAfter(dds.get(5).text(), "시가");
        String highPrice = extractAfter(dds.get(6).text(), "고가");
        String upperLimit = extractAfter(dds.get(7).text(), "상한가");
        String lowPrice = extractAfter(dds.get(8).text(), "저가");
        String lowerLimit = extractAfter(dds.get(9).text(), "하한가");
        String volume = extractAfter(dds.get(10).text(), "거래량");

        // 백만만 처리 : 1,123백만 -> 1,123,000,000
        String raw = extractAfter(dds.get(11).text(), "거래대금").trim();

        String numberPart = raw.replaceAll("[^0-9,]", "");
        String unitPart = raw.replaceAll("[0-9,]", "");

        long value = Long.parseLong(numberPart.replace(",", ""));

        if (unitPart.contains("백만")) {
            value *= 1_000_000L;
        }

        String tradingValue = String.format("%,d", value);

        return StockDetailTickMessage.of(
                code,
                stockName,
                market,
                baseTime,
                priceInfo.currentPrice(),
                priceInfo.diffPrice(),
                priceInfo.diffRate(),
                priceInfo.direction(),
                prevPrice,
                openPrice,
                highPrice,
                upperLimit,
                lowPrice,
                lowerLimit,
                volume,
                tradingValue
        );
    }

    private PriceInfo parsePriceInfo(String text) {

        String direction = "STEADY";

        if (text.contains("상승")) {
            direction = "UP";
        } else if (text.contains("하락")) {
            direction = "DOWN";
        }

        String currentPrice = text.replaceAll("현재가\\s*([0-9,]+).*", "$1");

        String diffPrice = "0";
        if (text.matches(".*(상승|하락).*")) {
            diffPrice = text.replaceAll(".*전일대비\\s*(상승|하락)\\s*([0-9,]+).*", "$2");
        }

        String diffRate = text.replaceAll(".*([0-9]+\\.[0-9]+)\\s*퍼센트.*", "$1");

        return new PriceInfo(currentPrice, diffPrice, diffRate, direction);
    }

    private record PriceInfo(String currentPrice, String diffPrice, String diffRate, String direction) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverPopularStockItem(
            String itemcode,
            String itemname,
            String nowPrice,
            String upDownGb
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverForeignTrendItem(
            String itemCode,
            String bizdate,
            String organPureBuyQuant,
            String individualPureBuyQuant,
            String foreignerPureBuyQuant,
            String frgnStock,
            String frgnHoldRatio,
            String closePrice,
            String prevChangePrice,
            String upDownGb,
            String tradeVolume
    ) {
    }


    // 종목 상세 (REST 부가정보)

    public StockDetailExtraResponse getStockDetailExtra(String code) {

        Document doc = fetchDetailDocument(code);

        String companySummary = parseCompanySummary(doc);
        String warningType = parseWarningType(doc);
        String manage = parseManage(doc);
        List<BrokerTradeInfo> brokerTrades = parseBrokerTrades(doc);
        ForeignBrokerSummary foreignBrokerSummary = parseForeignBrokerSummary(doc);

        return new StockDetailExtraResponse(
                code,
                companySummary,
                warningType,
                manage,
                brokerTrades,
                foreignBrokerSummary
        );
    }

    private Document fetchDetailDocument(String code) {
        try {
            String html = getHtml(code);
            return Jsoup.parse(html);
        } catch (RestClientResponseException e) {
            log.error("Naver 종목 상세 크롤링 실패. status={}, code={}", e.getStatusCode(), code);
            throw new IllegalStateException("네이버 종목 상세 크롤링 실패", e);
        }
    }

    private String parseCompanySummary(Document doc) {

        Elements ps = doc.select("#summary_info p");

        StringBuilder sb = new StringBuilder();

        for (Element p : ps) {
            String text = p.text().trim();
            if (!text.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append(text);
            }
        }

        return sb.toString();
    }

    private String parseWarningType(Document doc) {
        for (String cls : List.of("caution", "warning", "danger")) {
            Element em = doc.selectFirst(".description em." + cls);

            if (em == null) continue;

            Element blind = em.selectFirst("span.blind");

            if (blind != null && !blind.text().isBlank()) {
                return blind.text().trim();
            }

            String text = em.text().trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String parseManage(Document doc) {
        Element manage = doc.selectFirst(".description em.manage");
        return manage == null ? "" : manage.text().trim();
    }

    private List<BrokerTradeInfo> parseBrokerTrades(Document doc) {

        List<BrokerTradeInfo> result = new ArrayList<>();

        Element investTrend = doc.selectFirst("div.invest_trend");
        if (investTrend == null) {
            return result;
        }

        Element table = investTrend.selectFirst("table.tb_type1");
        if (table == null) {
            return result;
        }

        Elements rows = table.select("tbody tr");

        for (Element row : rows) {

            Elements tds = row.select("td");
            if (tds.size() != 4) {
                continue;
            }

            result.add(new BrokerTradeInfo(
                    tds.get(0).text(),
                    tds.get(1).text(),
                    tds.get(2).text(),
                    tds.get(3).text(),
                    extractDirection(tds.get(0)),
                    extractDirection(tds.get(1)),
                    extractDirection(tds.get(2)),
                    extractDirection(tds.get(3))
            ));
        }

        return result;
    }

    private String extractDirection(Element td) {

        if (td == null) {
            return "";
        }
        if (!td.select(".f_up").isEmpty()) {
            return "UP";
        }
        if (!td.select(".f_down").isEmpty()) {
            return "DOWN";
        }
        return "";
    }

    private ForeignBrokerSummary parseForeignBrokerSummary(Document doc) {

        Element row = doc.selectFirst(".invest_trend table.tb_type1 tfoot tr");
        if (row == null) {
            return null;
        }

        Elements tds = row.select("td");
        if (tds.size() != 4) {
            return null;
        }

        Element sellEm = tds.get(1).selectFirst("em");
        Element buyDiffEm = tds.get(2).selectFirst("em");
        Element buyVolEm = tds.get(3).selectFirst("em");

        return new ForeignBrokerSummary(
                tds.get(0).text(),
                tds.get(1).text(),
                tds.get(2).text(),
                tds.get(3).text(),
                extractClass(sellEm),
                extractClass(buyDiffEm),
                extractClass(buyVolEm)
        );
    }

    private String extractClass(Element element) {

        if (element == null) {
            return "";
        }
        if (element.hasClass("f_up")) {
            return "UP";
        }
        if (element.hasClass("f_down")) {
            return "DOWN";
        }
        return "";
    }

    private String extractAfter(String text, String prefix) {

        if (text == null) {
            return "";
        }
        return text.replace(prefix, "").trim();
    }

    public List<ExchangeRateDto> getExchangeRates() {

        List<NaverMarketIndexItem> items;
        try {
            items = stockApiClient.get()
                    .uri("/api/securityService/marketindex/majors/rpc")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NaverMarketIndexItem>>() {});
        } catch (RestClientResponseException e) {
            log.error("Naver 환율 API 호출 실패. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 환율 크롤링 실패", e);
        }

        if (items == null) {
            return new ArrayList<>();
        }

        List<ExchangeRateDto> result = new ArrayList<>();
        for (NaverMarketIndexItem item : items) {
            if (!"exchange".equals(item.categoryType())) {
                continue;
            }
            result.add(mapExchangeRate(item));
        }

        return result;
    }

    private ExchangeRateDto mapExchangeRate(NaverMarketIndexItem item) {

        String direction = item.fluctuationsType() != null
                ? item.fluctuationsType().text()
                : "보합";

        String time = item.localTradedAt() != null ? item.localTradedAt() : "";

        return new ExchangeRateDto(
                item.name(),
                item.symbolCode(),
                item.closePrice(),
                item.fluctuations(),
                direction,
                time,
                item.endUrl()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverMarketIndexItem(
            String categoryType,
            String reutersCode,
            String symbolCode,
            String name,
            String closePrice,
            String fluctuations,
            String fluctuationsRatio,
            NaverFluctuationsType fluctuationsType,
            String localTradedAt,
            String endUrl
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverFluctuationsType(
            String code,
            String text,
            String name
    ) {
    }

    public List<WorldIndexDto> getWorldIndexes() {

        String html;
        try {
            html = restClient.get()
                    .uri("/world/")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 세계증시 크롤링 실패. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 세계증시 크롤링 실패", e);
        }

        Document doc = Jsoup.parse(html);
        Elements indexes = doc.select(
                "#worldIndexColumn1 li, #worldIndexColumn2 li, #worldIndexColumn3 li"
        );

        List<WorldIndexDto> result = new ArrayList<>();

        for (Element index : indexes) {

            Element dl = index.selectFirst("dl");
            if (dl == null) {
                continue;
            }

            Element nameElement = dl.selectFirst("dt span.blind");
            Element pointStatus = dl.selectFirst("dd.point_status");
            Element dateElement = dl.selectFirst("dd.date em");
            Element linkElement = dl.selectFirst("dt a");

            if (nameElement == null || pointStatus == null
                    || dateElement == null || linkElement == null) {
                continue;
            }

            String name = nameElement.text().trim();
            String currentPrice = pointStatus.selectFirst("strong").text().trim();
            String diffPrice = pointStatus.selectFirst("em").text().trim();

            Element spanElement = pointStatus.selectFirst("span");
            String rawChangeText = spanElement != null ? spanElement.text().trim() : "";

            String direction;
            if (rawChangeText.contains("+")) {
                direction = "상승";
            } else if (rawChangeText.contains("-")) {
                direction = "하락";
            } else {
                direction = "보합";
            }

            String diffRate = rawChangeText.replace("+", "").replace("-", "").trim();
            String dateTime = dateElement.text().trim();
            String detailUrl = linkElement.attr("href");

            result.add(new WorldIndexDto(
                    name, currentPrice, diffPrice, diffRate, direction, dateTime, detailUrl
            ));
        }

        return result;
    }

    public TrendResponse getInvestorTrend(MarketType market, TrendType type, int page) {

        int pageSize = 30;
        int startIdx = page - 1;
        String bizDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        NaverInvestorTrendApiResponse response;
        try {
            response = stockApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/domestic/market/trend/{path}")
                            .queryParam("tradeType", "KRX")
                            .queryParam("marketType", market.getCode())
                            .queryParam("bizdate", bizDate)
                            .queryParam("startIdx", startIdx)
                            .queryParam("pageSize", pageSize)
                            .build(type.getPath()))
                    .retrieve()
                    .body(NaverInvestorTrendApiResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 투자자별 매매동향 API 호출 실패. status={}, market={}, type={}, page={}",
                    e.getStatusCode(), market, type, page);
            throw new IllegalStateException("네이버 투자자별 매매동향 크롤링 실패", e);
        }

        if (response == null || response.content() == null) {
            return new TrendResponse(new ArrayList<>(), false);
        }

        List<InvestorTrendDto> data = new ArrayList<>();
        for (NaverInvestorTrendContent content : response.content()) {
            data.add(mapInvestorTrend(content, type));
        }

        boolean hasNext = !response.last();

        return new TrendResponse(data, hasNext);
    }

    private InvestorTrendDto mapInvestorTrend(NaverInvestorTrendContent content, TrendType type) {

        Map<String, Long> amounts = new HashMap<>();
        if (content.netAmounts() != null) {
            for (NaverInvestorNetAmount item : content.netAmounts()) {
                amounts.put(item.investorGubun(), parseLongSafely(item.diffValue()));
            }
        }

        long financeInvestment = amounts.getOrDefault("1000", 0L);
        long insurance = amounts.getOrDefault("2000", 0L);
        long fund = amounts.getOrDefault("3000", 0L) + amounts.getOrDefault("3100", 0L);
        long bank = amounts.getOrDefault("4000", 0L);
        long etcFinance = amounts.getOrDefault("5000", 0L);
        long pension = amounts.getOrDefault("6000", 0L);
        long corporation = amounts.getOrDefault("7000", 0L) + amounts.getOrDefault("7100", 0L);
        long individual = amounts.getOrDefault("8000", 0L);
        long foreigner = amounts.getOrDefault("9000", 0L) + amounts.getOrDefault("9001", 0L);

        long institution = financeInvestment + insurance + fund + bank + etcFinance + pension;

        String dateOrTime = type == TrendType.TIME ? content.time() : content.bizdate();

        return new InvestorTrendDto(
                dateOrTime,
                individual,
                foreigner,
                institution,
                financeInvestment,
                insurance,
                fund,
                bank,
                etcFinance,
                pension,
                corporation
        );
    }

    private InvestorTrendDto mapInvestorTrend(NaverInvestorTrendContent content) {

        Map<String, Long> amounts = new HashMap<>();
        if (content.netAmounts() != null) {
            for (NaverInvestorNetAmount item : content.netAmounts()) {
                amounts.put(item.investorGubun(), parseLongSafely(item.diffValue()));
            }
        }

        long financeInvestment = amounts.getOrDefault("1000", 0L);
        long insurance = amounts.getOrDefault("2000", 0L);
        long fund = amounts.getOrDefault("3000", 0L) + amounts.getOrDefault("3100", 0L);
        long bank = amounts.getOrDefault("4000", 0L);
        long etcFinance = amounts.getOrDefault("5000", 0L);
        long pension = amounts.getOrDefault("6000", 0L);
        long corporation = amounts.getOrDefault("7000", 0L) + amounts.getOrDefault("7100", 0L);
        long individual = amounts.getOrDefault("8000", 0L);
        long foreigner = amounts.getOrDefault("9000", 0L) + amounts.getOrDefault("9001", 0L);

        long institution = financeInvestment + insurance + fund + bank + etcFinance + pension;

        String dateOrTime = (content.bizdate() != null && !content.bizdate().isBlank())
                ? content.bizdate()
                : content.time();

        return new InvestorTrendDto(
                dateOrTime,
                individual,
                foreigner,
                institution,
                financeInvestment,
                insurance,
                fund,
                bank,
                etcFinance,
                pension,
                corporation
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverInvestorTrendApiResponse(
            List<NaverInvestorTrendContent> content,
            boolean last
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverInvestorTrendContent(
            String bizdate,
            String time,
            List<NaverInvestorNetAmount> netAmounts
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverInvestorNetAmount(
            String investorGubun,
            String diffValue,
            String sellQuant,
            String sellPrice,
            String buyQuant,
            String buyPrice
    ) {
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }

        return Long.parseLong(
                value.replace(",", "")
                        .replace("+", "")
                        .trim()
        );
    }

    private static final int DEAL_RANK_PAGE_SIZE = 50;

    public DealRankResponse getDealRank(DealRankMarket market, InvestorType investorType, DealType dealType, PeriodType periodType) {

        NaverForeignOrgTrendResponse response;
        try {
            response = stockApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/domestic/market/trend/trendForeignOrg")
                            .queryParam("investorType", investorType.getCode())
                            .queryParam("tradeType", "KRX")
                            .queryParam("marketType", market.getCode())
                            .queryParam("startIdx", 0)
                            .queryParam("pageSize", DEAL_RANK_PAGE_SIZE)
                            .queryParam("periodType", periodType.getCode())
                            .build())
                    .retrieve()
                    .body(NaverForeignOrgTrendResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 수급 순위 API 호출 실패. status={}, market={}, investorType={}, dealType={}, periodType={}",
                    e.getStatusCode(), market, investorType, dealType, periodType);
            throw new IllegalStateException("네이버 수급 순위 크롤링 실패", e);
        }

        if (response == null || response.sections() == null) {
            return new DealRankResponse(market.name(), investorType.name(), dealType.name(), periodType.name(), new ArrayList<>());
        }

        List<NaverForeignOrgRankItem> items = dealType == DealType.BUY
                ? response.sections().buyRankList()
                : response.sections().sellRankList();

        if (items == null) {
            items = new ArrayList<>();
        }

        String dealDate = items.isEmpty()
                ? ""
                : formatDealDateRange(items.get(0).bizdateFrom(), items.get(0).bizdateTo());

        List<DealRankItem> dealRankItems = new ArrayList<>();
        int rank = 1;
        for (NaverForeignOrgRankItem item : items) {
            dealRankItems.add(new DealRankItem(
                    rank++,
                    item.itemcode(),
                    item.itemname(),
                    toBigDecimalSafely(item.accTradeVolume()),
                    toBigDecimalSafely(item.accTradeAmount()),
                    toBigDecimalSafely(item.dailyTradeVolume())
            ));
        }

        DealRankDay day = new DealRankDay(dealDate, dealRankItems);

        return new DealRankResponse(market.name(), investorType.name(), dealType.name(), periodType.name(), List.of(day));
    }

    private String formatDealDateRange(String bizdateFrom, String bizdateTo) {
        String from = formatBizDate(bizdateFrom);
        String to = formatBizDate(bizdateTo);
        if (from.equals(to)) {
            return from;
        }
        return from + " ~ " + to;
    }

    private BigDecimal toBigDecimalSafely(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverForeignOrgTrendResponse(
            NaverForeignOrgSections sections
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverForeignOrgSections(
            List<NaverForeignOrgRankItem> buyRankList,
            List<NaverForeignOrgRankItem> sellRankList
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverForeignOrgRankItem(
            String itemcode,
            String itemname,
            String bizdateFrom,
            String bizdateTo,
            String accTradeVolume,
            String accTradeAmount,
            String dailyTradeVolume,
            String nowPrice,
            String prevChangeRate,
            String prevChangePrice,
            String type
    ) {
    }

    private static final Pattern DEAL_RANK_CODE_PATTERN = Pattern.compile("code=([^&\"]+)");

    private int indexOf(List<String> colNames, String... keywords) {
        for (int i = 0; i < colNames.size(); i++) {
            for (String kw : keywords) {
                if (colNames.get(i).contains(kw)) return i;
            }
        }
        return -1;
    }

    private BigDecimal tdNumber(Elements tds, int idx, int fallback) {
        int target = (idx >= 0 && idx < tds.size()) ? idx : fallback;
        if (target < 0 || target >= tds.size()) return null;
        return toDealRankNumber(tds.get(target).text());
    }

    private String extractDealRankCode(Element anchor) {
        if (anchor == null) return null;
        String href = anchor.attr("href");
        Matcher m = DEAL_RANK_CODE_PATTERN.matcher(href);
        return m.find() ? m.group(1) : null;
    }

    private BigDecimal toDealRankNumber(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replace(",", "").trim();
        if (cleaned.isEmpty() || cleaned.equals("-")) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

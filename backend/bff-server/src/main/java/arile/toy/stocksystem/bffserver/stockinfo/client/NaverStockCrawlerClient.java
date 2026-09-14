package arile.toy.stocksystem.bffserver.stockinfo.client;

import arile.toy.stocksystem.bffserver.stockinfo.dto.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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

        NaverStockDetailResponse d = fetchDetail(code);
        NaverConsensusResponse consensus = fetchConsensus(code);

        String opinion = consensus != null ? consensus.opinion() : "";
        String targetPrice = consensus != null ? formatComma(consensus.targetPrice()) : "";
        String consensusDate = consensus != null ? formatBizDate(consensus.date()) : "";

        return new StockInfo(
                formatComma(d.marketSum()),
                formatComma(d.listedStockCnt()),

                d.facePrice(),
                d.unit(),

                formatComma(d.frgnLimitCnt()),
                formatComma(d.frgnHoldCnt()),
                formatPercent(d.frgnAcqRatio()),

                opinion,
                targetPrice,
                consensusDate,

                formatComma(d.week52HighPrice()),
                formatComma(d.week52LowPrice()),

                d.per(),
                d.eps(),

                d.estimatedPer(),
                d.estimatedEps(),

                d.pbr(),
                d.bps(),

                formatPercent(d.dividendRate()),

                d.sameIndustryPer(),
                formatSignedRate(d.sameIndustryChangeRate())
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
                            .queryParam("pageSize", 50)
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

    public MarketMainResponse getMarketIndices() {

        MarketIndexInfo kospi = fetchMarketIndex("KOSPI", "코스피");
        MarketIndexInfo kosdaq = fetchMarketIndex("KOSDAQ", "코스닥");
        MarketIndexInfo kospi200 = fetchMarketIndex("KPI200", "코스피200");

        return new MarketMainResponse(kospi, kosdaq, kospi200);
    }

    private MarketIndexInfo fetchMarketIndex(String code, String name) {

        NaverIndexBasicResponse basic;
        NaverIndexIntegrationResponse integration;

        try {
            basic = stockApiClient.get()
                    .uri("/api/securityFe/api/index/{code}/basic", code)
                    .retrieve()
                    .body(NaverIndexBasicResponse.class);

            integration = stockApiClient.get()
                    .uri("/api/securityFe/api/index/{code}/integration", code)
                    .retrieve()
                    .body(NaverIndexIntegrationResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 지수 API 호출 실패. status={}, code={}", e.getStatusCode(), code);
            throw new IllegalStateException("네이버 지수 크롤링 실패", e);
        }

        if (basic == null || integration == null) {
            throw new IllegalStateException("네이버 지수 응답이 비어 있습니다. code=" + code);
        }

        return mapMarketIndex(basic, integration, name);
    }

    private MarketIndexInfo mapMarketIndex(NaverIndexBasicResponse basic, NaverIndexIntegrationResponse integration, String name) {

        Map<String, String> totalInfos = new HashMap<>();
        if (integration.totalInfos() != null) {
            for (NaverIndexTotalInfo info : integration.totalInfos()) {
                totalInfos.put(info.code(), info.value());
            }
        }

        String currentIndex = basic.closePrice();
        String changeValue = basic.compareToPreviousClosePrice();
        String changeRate = basic.fluctuationsRatio();
        String direction = basic.compareToPreviousPrice() != null
                ? mapFluctuationsDirection(basic.compareToPreviousPrice().name())
                : "";
        String baseTime = basic.localTradedAt() != null ? basic.localTradedAt() : "";

        String prevClose = totalInfos.get("lastClosePrice");
        String openPrice = totalInfos.get("openPrice");
        String highPrice = totalInfos.get("highPrice");
        String lowPrice = totalInfos.get("lowPrice");
        String high52Weeks = totalInfos.get("highPriceOf52Weeks");
        String low52Weeks = totalInfos.get("lowPriceOf52Weeks");
        String volume = totalInfos.get("accumulatedTradingVolume");
        String tradingValue = totalInfos.get("accumulatedTradingValue");

        MarketBreadth breadth = integration.upDownStockInfo() != null
                ? new MarketBreadth(
                integration.upDownStockInfo().upperCount(),
                integration.upDownStockInfo().riseCount(),
                integration.upDownStockInfo().steadyCount(),
                integration.upDownStockInfo().fallCount(),
                integration.upDownStockInfo().lowerCount(),
                null
        )
                : null;

        ProgramTrade programTrade = integration.programTrendInfo() != null
                ? new ProgramTrade(
                integration.programTrendInfo().indexDifferenceReal(),
                integration.programTrendInfo().indexBiDifferenceReal(),
                integration.programTrendInfo().indexTotalReal()
        )
                : null;

        InvestorTrend investorTrend = integration.dealTrendInfo() != null
                ? new InvestorTrend(
                integration.dealTrendInfo().personalValue(),
                integration.dealTrendInfo().foreignValue(),
                integration.dealTrendInfo().institutionalValue()
        )
                : null;

        return new MarketIndexInfo(
                name,
                currentIndex,
                changeValue,
                changeRate,
                direction,
                baseTime,
                prevClose,
                openPrice,
                highPrice,
                lowPrice,
                high52Weeks,
                low52Weeks,
                volume,
                tradingValue,
                breadth,
                programTrade,
                investorTrend
        );
    }

    private String mapFluctuationsDirection(String name) {
        return switch (name) {
            case "RISING" -> "UP";
            case "FALLING" -> "DOWN";
            default -> "STEADY";
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverIndexBasicResponse(
            String itemCode,
            String stockName,
            String closePrice,
            String compareToPreviousClosePrice,
            NaverFluctuationsType compareToPreviousPrice,
            String fluctuationsRatio,
            String localTradedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverIndexIntegrationResponse(
            String itemCode,
            String stockName,
            List<NaverIndexTotalInfo> totalInfos,
            NaverDealTrendInfo dealTrendInfo,
            NaverProgramTrendInfo programTrendInfo,
            NaverUpDownStockInfo upDownStockInfo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverIndexTotalInfo(
            String code,
            String key,
            String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverDealTrendInfo(
            String bizdate,
            String personalValue,
            String foreignValue,
            String institutionalValue
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverProgramTrendInfo(
            String bizdate,
            String indexBiDifferenceReal,
            String indexTotalReal,
            String indexDifferenceReal
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverUpDownStockInfo(
            String upperCount,
            String riseCount,
            String lowerCount,
            String fallCount,
            String steadyCount
    ) {
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

    public StockDetailTickMessage getStockDetailSummary(String code) {

        NaverStockDetailResponse d = fetchDetail(code);
        String market = fetchSosok(code);

        String direction = mapDirection(d.upDownGb()); // 기존 PopularStock 매핑 재사용

        return StockDetailTickMessage.of(
                d.itemcode(),
                d.itemname(),
                market,
                d.tradeTime(),
                formatComma(d.nowPrice()),
                formatComma(d.prevChangePrice()),
                formatSignedRate(d.prevChangeRate()),
                direction,
                formatComma(d.prevClosePrice()),
                formatComma(d.openPrice()),
                formatComma(d.highPrice()),
                formatComma(d.upperLimitPrice()),
                formatComma(d.lowPrice()),
                formatComma(d.lowerLimitPrice()),
                formatComma(d.tradeVolume()),
                formatComma(d.tradeAmount())
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverSosokResponse(String sosok, String isNxtYn) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverStockDetailResponse(
            String itemcode, String itemname, String sosok, String upJongName,
            String tradeTime, String manageStatusGb, String marketAlertType,
            String nowPrice, String openPrice, String prevClosePrice, String stdPrice,
            String highPrice, String lowPrice, String upperLimitPrice, String lowerLimitPrice,
            String upDownGb, String prevChangePrice, String prevChangeRate,
            String tradeVolume, String tradeAmount, String marketSum,
            String listedStockCnt, String facePrice, String unit,
            String frgnLimitCnt, String frgnHoldCnt, String frgnAcqRatio, String frgnHoldRate,
            String per, String eps, String estimatedPer, String estimatedEps,
            String pbr, String bps, String dividendRate,
            String week52HighPrice, String week52LowPrice,
            String comment1, String comment2, String comment3,
            String sameIndustryPer, String sameIndustryChangeRate
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverTraderInfoResponse(
            String sellQuant, String buyQuant, String quant,
            List<NaverTraderItem> traderList
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverTraderItem(
            String traderNo, String nameKr, String display_name,
            String sellQuant, String buyQuant, String quant
    ) {}

    private String fetchSosok(String code) {
        try {
            NaverSosokResponse res = stockApiClient.get()
                    .uri("/api/domestic/detail/{code}/sosok", code)
                    .retrieve()
                    .body(NaverSosokResponse.class);
            return res != null ? res.sosok() : "";
        } catch (RestClientResponseException e) {
            log.warn("sosok 조회 실패. code={}", code);
            return "";
        }
    }

    private NaverStockDetailResponse fetchDetail(String code) {
        try {
            return stockApiClient.get()
                    .uri("/api/domestic/detail/{code}/detail?codeType=KRX", code)
                    .retrieve()
                    .body(NaverStockDetailResponse.class);
        } catch (RestClientResponseException e) {
            log.error("detail API 호출 실패. status={}, code={}", e.getStatusCode(), code);
            throw new IllegalStateException("네이버 종목 상세 API 실패", e);
        }
    }

    private NaverTraderInfoResponse fetchTraderInfo(String code) {
        try {
            return stockApiClient.get()
                    .uri("/api/domestic/detail/{code}/traderInfo", code)
                    .retrieve()
                    .body(NaverTraderInfoResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("traderInfo 조회 실패. code={}", code);
            return null;
        }
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

    public StockDetailExtraResponse getStockDetailExtra(String code) {

        NaverStockDetailResponse d = fetchDetail(code);
        String market = fetchSosok(code);
        NaverTraderInfoResponse trader = fetchTraderInfo(code);

        String companySummary = String.join("\n",
                List.of(d.comment1(), d.comment2(), d.comment3()).stream()
                        .filter(s -> s != null && !s.isBlank())
                        .toList());

        String warningType = mapWarningType(d.marketAlertType());
        String manage = mapManageStatus(d.manageStatusGb());

        List<BrokerTradeInfo> brokerTrades = mapBrokerTrades(trader);
        ForeignBrokerSummary foreignBrokerSummary = mapForeignBrokerSummary(trader);

        return new StockDetailExtraResponse(
                code,
                market,
                companySummary,
                warningType,
                manage,
                brokerTrades,
                foreignBrokerSummary
        );
    }

    private String mapWarningType(String marketAlertType) {
        if (marketAlertType == null) return "";
        return switch (marketAlertType) {
            case "00" -> "";
            case "01" -> "투자주의";
            case "02" -> "투자경고";
            case "03" -> "투자위험";
            default -> "";
        };
    }

    private String mapManageStatus(String manageStatusGb) {
        if (manageStatusGb == null || manageStatusGb.equals("0")) return "";
        return "관리종목";
    }

    private List<BrokerTradeInfo> mapBrokerTrades(NaverTraderInfoResponse trader) {
        if (trader == null || trader.traderList() == null) return new ArrayList<>();

        List<NaverTraderItem> sellTop = trader.traderList().stream()
                .sorted((a, b) -> Long.compare(parseLongSafely(b.sellQuant()), parseLongSafely(a.sellQuant())))
                .limit(5).toList();

        List<NaverTraderItem> buyTop = trader.traderList().stream()
                .sorted((a, b) -> Long.compare(parseLongSafely(b.buyQuant()), parseLongSafely(a.buyQuant())))
                .limit(5).toList();

        List<BrokerTradeInfo> result = new ArrayList<>();
        int size = Math.max(sellTop.size(), buyTop.size());

        for (int i = 0; i < size; i++) {
            NaverTraderItem s = i < sellTop.size() ? sellTop.get(i) : null;
            NaverTraderItem b = i < buyTop.size() ? buyTop.get(i) : null;

            result.add(new BrokerTradeInfo(
                    s != null ? s.display_name() : "",
                    s != null ? formatComma(s.sellQuant()) : "",
                    b != null ? b.display_name() : "",
                    b != null ? formatComma(b.buyQuant()) : ""
            ));
        }
        return result;
    }

    private ForeignBrokerSummary mapForeignBrokerSummary(NaverTraderInfoResponse trader) {
        if (trader == null) return null;

        long quant = parseLongSafely(trader.quant());

        return new ForeignBrokerSummary(
                "외국계 합계",
                formatComma(trader.sellQuant()),
                formatSignedComma(trader.quant()),
                formatComma(trader.buyQuant())
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverConsensusResponse(
            String itemCode,
            String date,
            String opinion,
            String targetPrice
    ) {}

    private NaverConsensusResponse fetchConsensus(String code) {
        try {
            return stockApiClient.get()
                    .uri("/api/domestic/detail/{code}/consensus", code)
                    .retrieve()
                    .body(NaverConsensusResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("consensus 조회 실패. code={}", code);
            return null;
        }
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

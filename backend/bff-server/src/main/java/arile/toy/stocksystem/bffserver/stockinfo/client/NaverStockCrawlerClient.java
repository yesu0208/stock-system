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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class NaverStockCrawlerClient {

    private final RestClient restClient;

    public NaverStockCrawlerClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://finance.naver.com")
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
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



    public TradePageResponse getForeignInstitutionTrades(String code, int page) {

        List<ForeignInstitutionTrade> current = fetchTradePage(code, page);
        List<ForeignInstitutionTrade> next = fetchTradePage(code, page + 1);

        boolean hasNext = !next.isEmpty() && !isSamePage(current, next);

        return new TradePageResponse(current, hasNext);
    }

    private List<ForeignInstitutionTrade> fetchTradePage(String code, int page) {

        String html;
        try {
            html = restClient.get()
                    .uri("/item/frgn.naver?code={code}&page={page}", code, page)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver foreign trade crawling error. status={}, code={}, page={}",
                    e.getStatusCode(), code, page);
            throw new IllegalStateException("네이버 외국인/기관 매매동향 크롤링 실패", e);
        }

        Document doc = Jsoup.parse(html);
        List<ForeignInstitutionTrade> result = new ArrayList<>();

        Elements rows = doc.select("table.type2 tr");

        for (Element row : rows) {

            if (!row.hasAttr("onmouseover")) {
                continue;
            }

            Elements tds = row.select("td");

            if (tds.size() < 9) {
                continue;
            }

            result.add(new ForeignInstitutionTrade(
                    tds.get(0).text(),
                    tds.get(1).text(),
                    parseDiff(tds.get(2).text()),
                    tds.get(3).text(),
                    tds.get(4).text(),
                    tds.get(5).text(),
                    tds.get(6).text(),
                    tds.get(7).text(),
                    tds.get(8).text()
            ));
        }

        return result;
    }

    private boolean isSamePage(List<ForeignInstitutionTrade> a, List<ForeignInstitutionTrade> b) {

        if (a.size() != b.size()) {
            return false;
        }

        for (int i = 0; i < a.size(); i++) {

            ForeignInstitutionTrade x = a.get(i);
            ForeignInstitutionTrade y = b.get(i);

            if (!x.date().equals(y.date())
                    || !x.closePrice().equals(y.closePrice())
                    || !x.volume().equals(y.volume())) {
                return false;
            }
        }

        return true;
    }

    private String parseDiff(String text) {

        text = text.replace(",", "").trim();

        if (text.startsWith("상승")) {
            return "▲ " + text.replace("상승", "").trim();
        }
        if (text.startsWith("하락")) {
            return "▼ " + text.replace("하락", "").trim();
        }
        if (text.startsWith("보합0")) {
            return "0";
        }
        if (text.startsWith("상한가")) {
            return "⬆" + text.replace("상한가", "").trim();
        }
        if (text.startsWith("하한가")) {
            return "⬇" + text.replace("하한가", "").trim();
        }

        return text;
    }



    public MarketMainResponse getMarketIndices() {

        Document doc = fetchSiseDocument();

        MarketIndexInfo kospi = parseMarketIndex(doc, "KOSPI", "코스피", false);
        MarketIndexInfo kosdaq = parseMarketIndex(doc, "KOSDAQ", "코스닥", false);
        MarketIndexInfo kospi200 = parseMarketIndex(doc, "KPI200", "코스피200", true);

        return new MarketMainResponse(kospi, kosdaq, kospi200);
    }

    public List<PopularStock> getPopularStocks() {

        Document doc = fetchSiseDocument();

        return parsePopularStocks(doc);
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

    private List<PopularStock> parsePopularStocks(Document doc) {

        List<PopularStock> result = new ArrayList<>();

        Elements items = doc.select("#popularItemList li");

        for (Element item : items) {

            Element rankEl = item.selectFirst("em");
            Element a = item.selectFirst("a");

            if (rankEl == null || a == null) {
                continue;
            }

            int rank = Integer.parseInt(
                    rankEl.text().replace(".", "")
            );

            String href = a.attr("href");
            String code = href.substring(href.indexOf("code=") + 5);
            String name = a.text();

            Element priceEl = item.selectFirst("span.up, span.dn, span.nv, span.noc");

            if (priceEl == null) {
                log.warn("인기 종목 price element를 찾을 수 없음. html={}", item.outerHtml());
                continue;
            }

            String price = priceEl.text();

            Element blindEl = item.selectFirst("span.blind");
            String blindText = blindEl != null ? blindEl.text() : "";

            String direction;

            if (blindText.contains("상한가")) {
                direction = "UPPER_LIMIT";
            } else if (blindText.contains("하한가")) {
                direction = "LOWER_LIMIT";
            } else if (blindText.contains("상승")) {
                direction = "UP";
            } else if (blindText.contains("하락")) {
                direction = "DOWN";
            } else if (blindText.contains("보합")) {
                direction = "STEADY";
            } else {
                direction = "UNKNOWN";
            }

            result.add(new PopularStock(rank, code, name, price, direction));
        }

        return result;
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

        String html;
        try {
            html = restClient.get()
                    .uri("/sise/sise_group.naver?type=upjong")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 업종 목록 크롤링 실패. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 업종 목록 크롤링 실패", e);
        }

        Document doc = Jsoup.parse(html);
        List<UpjongInfo> result = new ArrayList<>();

        Elements rows = doc.select("table.type_1 tr");

        for (Element row : rows) {
            Elements tds = row.select("td");
            if (tds.isEmpty()) {
                continue;
            }

            Element a = tds.get(0).selectFirst("a");
            if (a == null) {
                continue;
            }

            String name = a.text().trim();

            String href = a.attr("href");
            String no = href.contains("no=")
                    ? href.substring(href.indexOf("no=") + 3)
                    : "";

            String changeRate = tds.get(1).text().trim();
            String total = tds.get(2).text().trim();
            String rise = tds.get(3).text().trim();
            String steady = tds.get(4).text().trim();
            String fall = tds.get(5).text().trim();
            String graphRatio = tds.get(6).text().trim();

            result.add(new UpjongInfo(
                    name, no, changeRate, total, rise, steady, fall, graphRatio
            ));
        }

        return new UpjongResponse(result);
    }

    public UpjongStockResponse getUpjongStocks(String upjongNo) {

        String html;
        try {
            html = restClient.get()
                    .uri("/sise/sise_group_detail.naver?type=upjong&no={no}", upjongNo)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 업종별 종목 크롤링 실패. status={}, no={}", e.getStatusCode(), upjongNo);
            throw new IllegalStateException("네이버 업종별 종목 크롤링 실패", e);
        }

        Document doc = Jsoup.parse(html);
        List<UpjongStock> result = new ArrayList<>();

        String upjongName = doc.select("h3.sub_tlt").text();

        Elements rows = doc.select("table.type_5 tr");

        for (Element row : rows) {
            Elements tds = row.select("td");
            if (tds.size() < 5) {
                continue;
            }

            Element a = tds.get(0).selectFirst("a");
            if (a == null) {
                continue;
            }

            String name = a.text();

            String href = a.attr("href");
            String code = href.contains("code=")
                    ? href.substring(href.indexOf("code=") + 5)
                    : "";

            String price = tds.get(1).text().trim();
            String change = tds.get(2).text().trim();
            String rate = tds.get(3).text().trim();

            result.add(new UpjongStock(name, code, price, change, rate));
        }

        return new UpjongStockResponse(upjongName, result);
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
        String tradingValue = extractAfter(dds.get(11).text(), "거래대금");

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
        Element warning = doc.selectFirst(".description em.warning");
        return warning == null ? "" : warning.text().trim();
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

        String html;
        try {
            html = restClient.get()
                    .uri("/marketindex/")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 환율 크롤링 실패. status={}", e.getStatusCode());
            throw new IllegalStateException("네이버 환율 크롤링 실패", e);
        }

        Document doc = Jsoup.parse(html);
        Elements items = doc.select("#exchangeList li");

        List<ExchangeRateDto> result = new ArrayList<>();

        for (Element li : items) {

            Element head = li.selectFirst("a.head");
            if (head == null) {
                continue;
            }

            String currencyName = head.selectFirst("h3 span.blind").text().trim();

            String currencyCode = head.classNames().stream()
                    .filter(c -> !c.equals("head"))
                    .findFirst()
                    .orElse("");

            Element info = head.selectFirst("div.head_info");
            String rate = info.selectFirst("span.value").text().trim();
            String change = info.selectFirst("span.change").text().trim();

            String direction;
            if (info.hasClass("point_up")) {
                direction = "상승";
            } else if (info.hasClass("point_dn")) {
                direction = "하락";
            } else {
                direction = "보합";
            }

            String time = li.selectFirst("div.graph_info span.time").text().trim();
            String detailUrl = head.attr("href");

            result.add(new ExchangeRateDto(
                    currencyName, currencyCode, rate, change, direction, time, detailUrl
            ));
        }

        return result;
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

    public List<InvestorTrendDto> getInvestorTrend(MarketType market, TrendType type, int page) {

        String url = buildInvestorTrendUri(market, type, page);

        String html;
        try {
            html = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 투자자별 매매동향 크롤링 실패. status={}, market={}, type={}, page={}",
                    e.getStatusCode(), market, type, page);
            throw new IllegalStateException("네이버 투자자별 매매동향 크롤링 실패", e);
        }

        return parseInvestorTrend(html);
    }

    private String buildInvestorTrendUri(MarketType market, TrendType type, int page) {

        String path = type == TrendType.TIME
                ? "/sise/investorDealTrendTime.naver"
                : "/sise/investorDealTrendDay.naver";

        String bizDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        String uri = path
                + "?bizdate=" + bizDate
                + "&sosok=" + market.getCode();

        if (page > 1) {
            uri += "&page=" + page;
        }

        return uri;
    }

    private List<InvestorTrendDto> parseInvestorTrend(String html) {

        Document doc = Jsoup.parse(html);
        List<InvestorTrendDto> result = new ArrayList<>();

        Elements rows = doc.select("table.type_1 tr");

        for (Element row : rows) {

            Elements tds = row.select("td");

            if (tds.size() != 11) {
                continue;
            }

            result.add(new InvestorTrendDto(
                    tds.get(0).text(),
                    parseLong(tds.get(1).text()),
                    parseLong(tds.get(2).text()),
                    parseLong(tds.get(3).text()),
                    parseLong(tds.get(4).text()),
                    parseLong(tds.get(5).text()),
                    parseLong(tds.get(6).text()),
                    parseLong(tds.get(7).text()),
                    parseLong(tds.get(8).text()),
                    parseLong(tds.get(9).text()),
                    parseLong(tds.get(10).text())
            ));
        }

        return result;
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

    public DealRankResponse getDealRank(DealRankMarket market, InvestorType investorType, DealType dealType) {

        String html;
        try {
            html = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sise/sise_deal_rank_iframe.naver")
                            .queryParam("sosok", market.getCode())
                            .queryParam("investor_gubun", investorType.getCode())
                            .queryParam("type", dealType.getCode())
                            .build())
                    .header(HttpHeaders.REFERER, "https://finance.naver.com/sise/sise_deal_rank.naver")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("Naver 수급 순위 크롤링 실패. status={}, market={}, investorType={}, dealType={}",
                    e.getStatusCode(), market, investorType, dealType);
            throw new IllegalStateException("네이버 수급 순위 크롤링 실패", e);
        }

        return parseDealRank(html, market, investorType, dealType);
    }

    private static final Pattern DEAL_RANK_CODE_PATTERN = Pattern.compile("code=([^&\"]+)");

    private DealRankResponse parseDealRank(String html, DealRankMarket market, InvestorType investorType, DealType dealType) {

        Document doc = Jsoup.parse(html);

        Elements dateBlocks = doc.select("div.box_type_ms");
        if (dateBlocks.isEmpty()) {
            throw new IllegalStateException("div.box_type_ms 를 찾을 수 없습니다. HTML 구조를 확인하세요.");
        }

        List<DealRankDay> days = new ArrayList<>();

        for (Element block : dateBlocks) {
            Element dateEl = block.selectFirst(".sise_guide_date");
            String dealDate = dateEl != null ? dateEl.text().trim() : "unknown";

            Element dataTable = null;
            for (Element table : block.select("table.type_1")) {
                if (!table.select("tbody tr").isEmpty()) {
                    dataTable = table;
                    break;
                }
            }
            if (dataTable == null) continue;

            Elements rows = dataTable.select("tbody tr");
            List<String> colNames = new ArrayList<>();
            for (Element tr : rows) {
                Elements ths = tr.select("th");
                if (!ths.isEmpty()) {
                    for (Element th : ths) colNames.add(th.text().trim());
                    break;
                }
            }

            int qtyIdx = indexOf(colNames, "수량");
            int amtIdx = indexOf(colNames, "금액");
            int volIdx = indexOf(colNames, "당일거래량", "거래량");

            List<DealRankItem> items = new ArrayList<>();
            int rank = 1;
            for (Element tr : rows) {
                Elements tds = tr.select("td");
                if (tds.isEmpty()) continue;
                Element anchor = tds.get(0).selectFirst("a");
                if (anchor == null) continue;

                String stockName = anchor.attr("title").trim();
                if (stockName.isEmpty()) stockName = anchor.text().trim();
                String stockCode = extractDealRankCode(anchor);

                BigDecimal quantity = tdNumber(tds, qtyIdx, 1);
                BigDecimal amount = tdNumber(tds, amtIdx, 2);
                BigDecimal volume = tdNumber(tds, volIdx, 3);

                items.add(new DealRankItem(rank++, stockCode, stockName, quantity, amount, volume));
            }

            days.add(new DealRankDay(dealDate, items));
        }

        return new DealRankResponse(market.name(), investorType.name(), dealType.name(), days);
    }

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
